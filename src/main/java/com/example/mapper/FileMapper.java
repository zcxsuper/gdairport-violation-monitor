package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.example.cache.MybatisRedisCache;
import com.example.domain.entity.File;
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@CacheNamespace(implementation = MybatisRedisCache.class)
public interface FileMapper extends BaseMapper<File> {

    /**
     * 使用 MySQL 8.0 递归 CTE 实现
     */
    List<File> listAllAncestors(@Param("id") Long id);

    List<File> listAllDescendants(@Param("id") Long id);
}
