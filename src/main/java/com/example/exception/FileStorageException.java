package com.example.exception;

import com.example.domain.ResultCode;
import lombok.Getter;

/**
 * 文件存储异常（500 Internal Server Error）
 * 用于处理文件上传、下载、删除等文件操作相关的错误
 * 这是一个运行时异常，因为在文件IO失败时，通常我们无法在业务代码中恢复，
 * 应该上抛给全局异常处理器记录日志并返回 500 错误。
 */
@Getter
public class FileStorageException extends RuntimeException {

    private final int code;

    public FileStorageException(String message) {
        super(message);
        this.code = ResultCode.SERVER_ERROR;
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.SERVER_ERROR;
    }

    public FileStorageException(int code, String message) {
        super(message);
        this.code = code;
    }

    public FileStorageException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

}