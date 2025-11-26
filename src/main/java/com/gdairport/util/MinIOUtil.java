package com.gdairport.util;

import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 简化版 MinIO 工具类（基于 Spring Bean 注入）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinIOUtil {

    private final MinioClient minioClient;  // 由 MinioConfig 注入

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    private static final String SEPARATOR = "/";

    /**
     * 获取上传文件前缀路径
     */
    public String getBasisUrl() {
        return endpoint + SEPARATOR + bucketName + SEPARATOR;
    }

    /** ================== Bucket 相关操作 ================== */

    /** 判断 Bucket 是否存在 */
    public boolean bucketExists(String bucket) throws Exception {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
    }

    /** 获取所有 Bucket */
    public List<Bucket> getAllBuckets() throws Exception {
        return minioClient.listBuckets();
    }

    /** 获取指定 Bucket */
    public Optional<Bucket> getBucket(String bucket) throws Exception {
        return getAllBuckets().stream().filter(b -> b.name().equals(bucket)).findFirst();
    }

    /** 删除 Bucket */
    public void removeBucket(String bucket) throws Exception {
        minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucket).build());
    }

    /** ================== 文件操作 ================== */

    /** 上传文件（MultipartFile） */
    public ObjectWriteResponse uploadFile(MultipartFile file, String objectName, String contentType) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            return minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .contentType(contentType)
                            .stream(inputStream, inputStream.available(), -1)
                            .build());
        }
    }

    /** 上传输入流 */
    public ObjectWriteResponse uploadFile(InputStream inputStream, String objectName) throws Exception {
        return minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, inputStream.available(), -1)
                        .build());
    }

    /** 获取文件流 */
    public InputStream getObject(String objectName) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(objectName).build());
    }

    /** 删除文件 */
    public void removeFile(String objectName) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
    }

    /** 列出指定前缀下所有文件 */
    public List<Item> listObjects(String prefix, boolean recursive) throws Exception {
        List<Item> list = new ArrayList<>();
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).prefix(prefix).recursive(recursive).build());
        for (Result<Item> result : results) {
            list.add(result.get());
        }
        return list;
    }

    /** 获取预签名文件URL */
    public String getPresignedObjectUrl(String objectName, int expires) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(expires)
                        .method(Method.GET)
                        .build());
    }

}
