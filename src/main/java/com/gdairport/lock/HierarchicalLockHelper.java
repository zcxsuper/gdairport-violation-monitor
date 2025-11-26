package com.gdairport.lock;

import com.gdairport.domain.entity.File;
import com.gdairport.mapper.FileMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HierarchicalLockHelper {

    private final RedissonClient redissonClient;
    private final FileMapper localFileMapper;

    /**
     * 获取从根到目标文件夹的所有祖先锁
     */
    public List<RReadWriteLock> getAncestorReadWriteLocks(Long parentId) {
        if (parentId == null) {
            return Collections.emptyList();
        }
        List<File> ancestors = localFileMapper.listAllAncestors(parentId);
        Collections.reverse(ancestors);
        // 为每个祖先节点生成 读写锁
        return ancestors.stream()
                .map(file -> redissonClient.getReadWriteLock("rwlock:file:" + file.getId()))
                .toList(); // 返回不可变 List
    }

    /**
     * 获取从目标文件夹到所有子孙文件/文件夹锁
     */
    public List<RReadWriteLock> getDescendantReadWriteLocks(Long id) {
        if (id == null) return Collections.emptyList();;
        // 一次性获取当前节点及所有子孙节点
        List<File> allNodes = localFileMapper.listAllDescendants(id);
        if (allNodes == null || allNodes.isEmpty()) {
            return Collections.emptyList();
        }

        return allNodes.stream()
                .map(file -> redissonClient.getReadWriteLock("rwlock:file:" + file.getId()))
                .toList(); // 返回不可变 List
    }

    /**
     * 锁定所有读锁 (用于下载等共享操作)
     * @param rwLocks 要锁定的读写锁列表
     * @return 组合锁 RLock (RedissonMultiLock)
     */
    // 锁定所有读锁 使用 RedissonMultiLock 锁定所有传入的锁
    public RLock lockAllRead(List<RReadWriteLock> rwLocks) {
        if (rwLocks == null || rwLocks.isEmpty()) {
            return null;
        }
        // 从每个 RReadWriteLock 中获取 readLock()
        RLock[] readLocks = rwLocks.stream()
                .map(RReadWriteLock::readLock)
                .toArray(RLock[]::new);

        RLock multiLock = redissonClient.getMultiLock(readLocks);
        multiLock.lock();
        return multiLock;
    }

    /**
     * 锁定所有写锁 (用于移动、重命名、删除等排他操作)
     * @param rwLocks 要锁定的读写锁列表
     * @return 组合锁 RLock (RedissonMultiLock)
     */
    public RLock lockAllWrite(List<RReadWriteLock> rwLocks) {
        if (rwLocks == null || rwLocks.isEmpty()) {
            return null;
        }
        // 从每个 RReadWriteLock 中获取 writeLock()
        RLock[] writeLocks = rwLocks.stream()
                .map(RReadWriteLock::writeLock)
                .toArray(RLock[]::new);

        RLock multiLock = redissonClient.getMultiLock(writeLocks);
        multiLock.lock();
        return multiLock;
    }

    /**
     * 通用解锁方法
     * (MultiLock 本身也是一个 RLock, 解锁逻辑不变)
     */
    public void unlockAll(RLock multiLock) {
        if (multiLock != null && multiLock.isHeldByCurrentThread()) {
            multiLock.unlock();
        }
    }
}
