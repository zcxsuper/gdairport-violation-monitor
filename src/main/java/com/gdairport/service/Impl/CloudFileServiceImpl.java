package com.gdairport.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.gdairport.domain.dto.CreateFolderDto;
import com.gdairport.domain.entity.File;
import com.gdairport.exception.BadRequestException;
import com.gdairport.exception.FileStorageException;
import com.gdairport.lock.HierarchicalLockHelper;
import com.gdairport.mapper.FileMapper;
import com.gdairport.service.CloudFileService;
import com.gdairport.util.MinIOUtil;
import io.minio.ObjectWriteResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.gdairport.exception.NotFoundException;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudFileServiceImpl extends ServiceImpl<FileMapper, File> implements CloudFileService {

    // 文件名非法字符正则表达式
    private static final Pattern INVALID_FILENAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

    // 文件名最大长度（对应数据库varchar(255)限制）
    private static final int MAX_FILENAME_LENGTH = 255;

    private final MinIOUtil minIOUtil;

    private final HierarchicalLockHelper hierarchicalLockHelper;

    @Value("${spring.servlet.multipart.max-file-size}")
    private DataSize maxFileSize;

    // 初始化后再注入、防止循环依赖
    @Lazy
    @Autowired
    private CloudFileServiceImpl cloudFileServiceImpl;

    @Override
    public File getFileById(Long id) {
        if (id == null || id <= 0) {
            throw new BadRequestException("文件ID无效");
        }
        return this.getById(id);
    }

    @Override
    public List<File> getFileListById(Long id) {
        if (id != null) {
            if (id <= 0) {
                throw new BadRequestException("文件夹ID无效");
            }
            File parent = this.getById(id);
            if (parent == null) {
                throw new NotFoundException(String.format("父文件夹不存在, id: %d", id));
            }
            if (!Boolean.TRUE.equals(parent.getFolder())) {
                throw new BadRequestException(String.format("指定的ID不是一个文件夹, id: %d", id));
            }
        }
        QueryWrapper<File> wrapper = new QueryWrapper<>();
        if (id == null) {
            wrapper.isNull("parent_id");
        } else {
            wrapper.eq("parent_id", id);
        }
        return this.list(wrapper);
    }

    @Override
    public void fileDownload(HttpServletResponse response, Long id) throws NotFoundException {
        if (id == null || id <= 0) {
            throw new BadRequestException("文件ID无效");
        }
        if (response == null) {
            throw new BadRequestException("响应对象不能为空");
        }

        File localFile = this.getById(id);
        if (localFile == null) {
            throw new NotFoundException(String.format("文件记录不存在, id: %d", id));
        }
        if (Boolean.TRUE.equals(localFile.getFolder())) {
            throw new BadRequestException(String.format("不能下载文件夹, id: %d", id));
        }
        String storageId = localFile.getStorageId();
        if (!StringUtils.hasText(localFile.getStorageId())) {
            throw new FileStorageException(String.format("文件存储ID无效, id: %d", id));
        }
        List<RReadWriteLock> ancestorLocks = new ArrayList<>();
        ancestorLocks.addAll(hierarchicalLockHelper.getAncestorReadWriteLocks(localFile.getParentId()));
        ancestorLocks.addAll(hierarchicalLockHelper.getDescendantReadWriteLocks(id));
        RLock multiLock = hierarchicalLockHelper.lockAllRead(ancestorLocks);
        try {
            response.reset();
            response.setCharacterEncoding("UTF-8");
            try {
                response.addHeader("Content-Disposition", "attachment;filename="
                        + URLEncoder.encode(localFile.getName(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                log.warn("文件名编码失败: {}", localFile.getName(), e);
                response.addHeader("Content-Disposition", "attachment;filename=file.dat");
            }
            // 设置为通用的二进制流类型，强制下载
            response.setContentType("application/octet-stream");
            response.setContentLengthLong(localFile.getSize());
            try (InputStream is = minIOUtil.getObject(storageId)) {
                // 使用 Java 9+ 的 transferTo 方法，自动处理缓冲区和读写
                is.transferTo(response.getOutputStream());
                // 确保响应流被刷新
                response.getOutputStream().flush();
            } catch (Exception e) {
                log.error("文件下载失败, storageId: {}", storageId, e);
                throw new FileStorageException(String.format("文件下载失败: %s", e.getMessage()), e);
            }
        } finally {
            hierarchicalLockHelper.unlockAll(multiLock);
        }
    }

    // 1. 先执行 I/O（上传） -> 2. 再获取锁 -> 3. 最后执行事务（写数据库）（失败回滚产生的孤儿数据节点可以定期清理）
    @Override
    public void fileUpload(MultipartFile file, Long parentId, Long userId) throws NotFoundException {

        if (file == null) {
            throw new BadRequestException("文件对象不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BadRequestException("用户ID无效");
        }
        long fileSize = file.getSize();
        if (fileSize <= 0) {
            throw new BadRequestException("文件大小无效");
        }
        if (fileSize > maxFileSize.toBytes()) {
            throw new BadRequestException(String.format(
                    "文件大小超过限制，最大允许 %.2fMB", (double) maxFileSize.toBytes() / (1024 * 1024)
            ));
        }
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BadRequestException("文件名不能为空");
        }
        validateFileName(originalFilename);
        long id = IdWorker.getId();
        String uuid = UUID.randomUUID().toString();
        try {
            ObjectWriteResponse response = minIOUtil.uploadFile(file, uuid, file.getContentType());
            log.info("文件成功上传到 MinIO. Object: {}, ETag: {}", response.object(), response.etag());
        } catch (Exception e) {
            log.error("文件上传到 MinIO 失败. ObjectName: {}", uuid, e);
            throw new FileStorageException(String.format("文件存储服务异常: %s", e.getMessage()), e);
        }
        List<RReadWriteLock> ancestorLocks = hierarchicalLockHelper.getAncestorReadWriteLocks(parentId);
        RLock multiLock = hierarchicalLockHelper.lockAllWrite(ancestorLocks);
        try {
            cloudFileServiceImpl.saveFileRecordWithTransaction(parentId, userId, id, originalFilename, fileSize, uuid);
        } catch (Exception e) {
            log.warn("数据库记录保存失败，MinIO 中可能存在孤儿文件: {}", uuid, e);
            if (e instanceof FileStorageException
                    || e instanceof BadRequestException
                    || e instanceof NotFoundException) {
                throw e;
            }
            throw new FileStorageException(String.format("保存文件记录时出错: %s", e.getMessage()), e);
        } finally {
            hierarchicalLockHelper.unlockAll(multiLock);
        }
    }

    @Transactional
    public void saveFileRecordWithTransaction(Long parentId, Long userId, long id, String originalFilename, long fileSize, String storageId) {

        if (parentId != null) {
            if (parentId <= 0) {
                throw new BadRequestException("父文件夹ID无效");
            }
            File parent = this.getById(parentId);
            if (parent == null) {
                throw new NotFoundException(String.format("父文件夹不存在 (可能在上传时被删除), id: %d", parentId));
            }
            if (!Boolean.TRUE.equals(parent.getFolder())) {
                throw new BadRequestException(String.format("指定的父ID不是一个文件夹, id: %d", parentId));
            }
        }
        checkDuplicateName(parentId, originalFilename, null);
        File fileRecord = File.builder()
                .id(id)
                .name(originalFilename)
                .parentId(parentId)
                .folder(false)
                .createdBy(userId)
                .updatedBy(userId)
                .size(fileSize)
                .storageId(storageId) // 存储 MinIO 的对象名称
                .build();
        if (!this.save(fileRecord)) {
            throw new FileStorageException("保存文件记录到数据库失败");
        }
    }


    @Override
    public void createFolder(CreateFolderDto createFolderDto, Long userId) throws NotFoundException {
        if (createFolderDto == null) {
            throw new BadRequestException("文件夹信息不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BadRequestException("用户ID无效");
        }
        // 验证文件夹名称合法性（包括长度检查，数据库限制为varchar(255)）
        if (createFolderDto.getName() == null || !StringUtils.hasText(createFolderDto.getName())) {
            throw new BadRequestException("文件夹名称不能为空");
        }
        validateFileName(createFolderDto.getName());
        Long parentId = createFolderDto.getParentId();
        // 获取父目录及祖先锁, 从顶级目录文件夹到父文件夹
        List<RReadWriteLock> ancestorLocks = hierarchicalLockHelper.getAncestorReadWriteLocks(parentId);
        RLock multiLock = hierarchicalLockHelper.lockAllWrite(ancestorLocks);
        try {
            cloudFileServiceImpl.createFolderWithTransaction(createFolderDto, userId);
        } finally {
            hierarchicalLockHelper.unlockAll(multiLock);
        }
    }

    @Transactional
    public void createFolderWithTransaction(CreateFolderDto createFolderDto, Long userId) {
        if (createFolderDto.getParentId() != null) {
            File parent = this.getById(createFolderDto.getParentId());
            if (parent == null) {
                throw new NotFoundException(String.format("父文件夹不存在, id: %d", createFolderDto.getParentId()));
            }
            if (!Boolean.TRUE.equals(parent.getFolder())) {
                throw new BadRequestException(String.format("指定的父ID不是一个文件夹, id: %d", createFolderDto.getParentId()));
            }
        }
        checkDuplicateName(createFolderDto.getParentId(), createFolderDto.getName(), null);
        File localFile = File.builder()
                .name(createFolderDto.getName())
                .parentId(createFolderDto.getParentId())
                .folder(true)
                .createdBy(userId)
                .updatedBy(userId)
                .size(0L)
                .build();
        if (!this.saveOrUpdate(localFile)) {
            throw new FileStorageException(String.format("文件夹创建失败: %s", createFolderDto.getName()));
        }
    }

    @Override
    public void rename(Long id, String newName, Long userId) throws NotFoundException {
        if (id == null || id <= 0) {
            throw new BadRequestException("文件ID无效");
        }
        if (!StringUtils.hasText(newName)) {
            throw new BadRequestException("新文件名不能为空");
        }
        validateFileName(newName);
        File localFile = this.getById(id);
        if (localFile == null) {
            throw new NotFoundException(String.format("文件或文件夹不存在, id: %s", id));
        }
        if (newName.equals(localFile.getName())) {
            throw new BadRequestException("新文件名与当前文件名相同，无需重命名");
        }
        List<RReadWriteLock> allLocks = new ArrayList<>();
        allLocks.addAll(hierarchicalLockHelper.getAncestorReadWriteLocks(localFile.getParentId()));
        allLocks.addAll(hierarchicalLockHelper.getDescendantReadWriteLocks(id));
        RLock multiLock = hierarchicalLockHelper.lockAllWrite(allLocks);
        try {
            cloudFileServiceImpl.renameWithTransaction(id, newName, userId, localFile);
        } finally {
            hierarchicalLockHelper.unlockAll(multiLock);
        }
    }

    @Transactional
    public void renameWithTransaction(Long id, String newName, Long userId, File localFile) {
        if (localFile == null) {
            localFile = this.getById(id);
            if (localFile == null) {
                throw new NotFoundException(String.format("文件在重命名时消失, id: %s", id));
            }
        }
        checkDuplicateName(localFile.getParentId(), newName, id);
        localFile.setName(newName.trim());
        localFile.setUpdatedBy(userId);
        if (!this.updateById(localFile)) {
            throw new FileStorageException(String.format("重命名失败, id: %s", id));
        }
    }

    @Override
    public void delete(Long id, Long userId) throws NotFoundException {
        if (id == null || id <= 0) {
            throw new BadRequestException("文件ID无效");
        }
        if (userId == null || userId <= 0) {
            throw new BadRequestException("用户ID无效");
        }
        File localFile = this.getById(id);
        if (localFile == null) {
            throw new NotFoundException(String.format("文件或文件夹不存在, id: %s", id));
        }
        List<RReadWriteLock> descendantLocks = hierarchicalLockHelper.getDescendantReadWriteLocks(id);
        RLock multiLock = hierarchicalLockHelper.lockAllWrite(descendantLocks);
        try {
            cloudFileServiceImpl.deleteWithTransaction(localFile, id);
        } catch (Exception e) {
            if (e instanceof FileStorageException || e instanceof NotFoundException) {
                throw e;
            }
            throw new FileStorageException(String.format("删除时发生未知错误: %s", e.getMessage()), e);
        } finally {
            hierarchicalLockHelper.unlockAll(multiLock);
        }
    }

    @Transactional
    public void deleteWithTransaction(File localFile, Long id) {
        if (Boolean.TRUE.equals(localFile.getFolder())) {
            deleteFolderRecursively(localFile.getId());
        } else {
            deletePhysicalFile(localFile);
        }
        if (!this.removeById(id)) {
            throw new FileStorageException(String.format("删除数据库记录失败: %s", id));
        }
    }

    @Override
    public void move(Long id, Long newParentId, Long userId) throws NotFoundException {
        if (id == null || id <= 0) {
            throw new BadRequestException("文件ID无效");
        }
        if (userId == null || userId <= 0) {
            throw new BadRequestException("用户ID无效");
        }
        File localFile = this.getById(id);
        if (localFile == null) {
            throw new NotFoundException(String.format("文件或文件夹不存在, id: %s", id));
        }
        Long currentParentId = localFile.getParentId();
        if ((newParentId == null && currentParentId == null) ||
                (newParentId != null && newParentId.equals(currentParentId))) {
            throw new BadRequestException("文件或文件夹已在目标位置，无需移动");
        }
        if (newParentId != null && newParentId.equals(id)) {
            throw new BadRequestException("不能将文件或文件夹移动到自身");
        }

        // 先加目标父文件夹及其祖先锁
        List<RReadWriteLock> locks = new ArrayList<>();
        if (newParentId != null) {
            locks.addAll(hierarchicalLockHelper.getAncestorReadWriteLocks(newParentId));
        }
        // 再加当前文件/文件夹及所有子孙锁
        locks.addAll(hierarchicalLockHelper.getDescendantReadWriteLocks(id));
        // 顺序加锁
        RLock multiLock = hierarchicalLockHelper.lockAllWrite(locks);
        try {
            cloudFileServiceImpl.moveWithTransaction(id, newParentId, userId, localFile);
        } finally {
            hierarchicalLockHelper.unlockAll(multiLock);
        }
    }

    @Transactional
    public void moveWithTransaction(Long id, Long newParentId, Long userId, File localFile) {
        if (newParentId != null) {
            File newParent = this.getById(newParentId);
            if (newParent == null) {
                throw new NotFoundException(String.format("目标父文件夹不存在, id: %s", newParentId));
            }
            if (!Boolean.TRUE.equals(newParent.getFolder())) {
                throw new BadRequestException(String.format("目标不是一个文件夹, id: %s", newParentId));
            }
            if (isAncestor(id, newParentId)) {
                throw new BadRequestException("不能将文件夹移动到其子文件夹中");
            }
            checkDuplicateName(newParentId, localFile.getName(), id);
        } else {
            checkDuplicateName(null, localFile.getName(), id);
        }

        localFile.setParentId(newParentId);
        localFile.setUpdatedBy(userId);
        if (!this.updateById(localFile)) {
            throw new FileStorageException(String.format("移动文件失败, id: %s", id));
        }
    }

    private void deleteFolderRecursively(Long folderId) {
        QueryWrapper<File> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", folderId);
        List<File> children = this.list(wrapper);
        if (children == null || children.isEmpty()) {
            return;
        }
        for (File child : children) {
            if (child == null || child.getId() == null) {
                continue;
            }
            try {
                if (Boolean.TRUE.equals(child.getFolder())) {
                    deleteFolderRecursively(child.getId());
                } else {
                    deletePhysicalFile(child);
                }
                boolean deleted = this.removeById(child.getId());
                if (!deleted) {
                    log.warn("删除数据库记录失败, id: {}", child.getId());
                }
            } catch (Exception e) {
                log.error("删除文件或文件夹时发生错误, id: {}, 错误: {}",
                        child.getId(), e.getMessage(), e);
            }
        }
    }

    private void deletePhysicalFile(File localFile) {
        if (localFile == null || !StringUtils.hasText(localFile.getStorageId())) {
            log.warn("文件记录 (ID: {}) 没有 storageId 或实体为空, 跳过 MinIO 删除.",
                    (localFile != null ? localFile.getId() : "null"));
            return;
        }
        String storageId = localFile.getStorageId();
        try {
            // 调用 MinIOUtil 删除对象 幂等
            minIOUtil.removeFile(storageId);
        } catch (Exception e) {
            log.error("从 MinIO 删除文件失败, storageId: {}, 错误: {}",
                    storageId, e.getMessage(), e);
            throw new FileStorageException("删除 MinIO 对象失败: " + localFile.getName(), e);
        }
    }

    /**
     * 检查folderId是否是targetId的祖先
     */
    private boolean isAncestor(Long folderId, Long targetId) {
        if (folderId == null || targetId == null) {
            return false;
        }
        if (folderId.equals(targetId)) {
            return false;
        }
        Long currentId = targetId;
        while (true) {
            File current = this.getById(currentId);
            if (current == null) {
                return false;
            }
            Long parentId = current.getParentId();

            if (parentId == null) {
                return false;
            }

            if (parentId.equals(folderId)) {
                return true;
            }
            currentId = parentId;
        }
    }

    private void validateFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new BadRequestException("文件名不能为空");
        }

        String trimmedName = fileName.trim();
        if (trimmedName.isEmpty()) {
            throw new BadRequestException("文件名不能只包含空格");
        }

        // 检查文件名长度（数据库字段限制为varchar(255)）
        if (trimmedName.length() > MAX_FILENAME_LENGTH) {
            throw new BadRequestException(String.format("文件名长度不能超过 %s 个字符（数据库限制）", MAX_FILENAME_LENGTH));
        }

        // 检查非法字符
        if (INVALID_FILENAME_CHARS.matcher(trimmedName).find()) {
            throw new BadRequestException("文件名包含非法字符，不允许使用: \\ / : * ? \" < > |");
        }

        // 检查文件名不能以点开头或结尾（特殊情况除外）
        if (trimmedName.startsWith(".") && trimmedName.length() == 1) {
            throw new BadRequestException("文件名不能只是一个点");
        }
        if (trimmedName.endsWith(".") && !trimmedName.equals("..")) {
            throw new BadRequestException("文件名不能以点结尾");
        }
    }


    private void checkDuplicateName(Long parentId, String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            return;
        }

        QueryWrapper<File> wrapper = new QueryWrapper<>();
        if (parentId == null) {
            wrapper.isNull("parent_id");
        } else {
            wrapper.eq("parent_id", parentId);
        }
        wrapper.eq("name", name.trim());

        // 排除当前文件（用于重命名和移动时的检查）
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }

        long count = this.count(wrapper);
        if (count > 0) {
            throw new BadRequestException(String.format("同一目录下已存在同名文件或文件夹: %s", name));
        }
    }

}
