package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.domain.dto.CreateFolderDto;
import com.example.domain.entity.File;
import com.example.domain.vo.StorageInfoVo;
import jakarta.servlet.http.HttpServletResponse;
import com.example.exception.NotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface CloudFileService extends IService<File> {

    File getFileById(Long id);

    List<File> getFileListById(Long id);

    void fileDownload(HttpServletResponse response, Long id) throws NotFoundException;

    void fileUpload(MultipartFile file, Long parentId, Long userId) throws NotFoundException;

    void createFolder(CreateFolderDto folderDto, Long userId) throws NotFoundException;

    void rename(Long id, String newName, Long userId) throws NotFoundException;

    void delete(Long id, Long userId) throws NotFoundException;

    void move(Long id, Long newParentId, Long userId) throws NotFoundException;
}
