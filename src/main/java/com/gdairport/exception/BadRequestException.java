package com.gdairport.exception;

import com.gdairport.domain.ResultCode;
import lombok.Getter;

/**
 * 自定义请求异常（400 Bad Request）
 * 用于处理客户端请求参数错误、验证失败等情况
 */
@Getter
public class BadRequestException extends RuntimeException {

    private final int code;

    public BadRequestException(String message) {
        super(message);
        this.code = ResultCode.BAD_REQUEST;
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.BAD_REQUEST;
    }

    public BadRequestException(int code, String message) {
        super(message);
        this.code = code;
    }

}
