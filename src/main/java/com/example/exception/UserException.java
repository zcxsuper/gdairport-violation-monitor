package com.example.exception;

import com.example.domain.ResultCode;
import lombok.Getter;

/**
 * 用户相关异常（400 Bad Request）
 * 用于处理用户注册、登录、用户信息等相关业务错误
 */
@Getter
public class UserException extends RuntimeException {

    private final int code;

    public UserException(String message) {
        super(message);
        this.code = ResultCode.BAD_REQUEST;
    }

    public UserException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.BAD_REQUEST;
    }

    public UserException(int code, String message) {
        super(message);
        this.code = code;
    }
}
