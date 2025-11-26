package com.gdairport.exception;

import com.gdairport.domain.ResultCode;
import lombok.Getter;

/**
 * 资源未找到异常（404 Not Found）
 * 用于处理请求的资源不存在的情况
 */
@Getter
public class NotFoundException extends RuntimeException {

    private final int code;

    public NotFoundException(String message) {
        super(message);
        this.code = ResultCode.NOT_FOUND;
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.NOT_FOUND;
    }

    public NotFoundException(int code, String message) {
        super(message);
        this.code = code;
    }

}

