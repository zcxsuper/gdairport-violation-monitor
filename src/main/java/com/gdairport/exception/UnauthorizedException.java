package com.gdairport.exception;

import com.gdairport.domain.ResultCode;
import lombok.Getter;

/**
 * 未授权异常（403 Forbidden）
 * 用于处理用户无权限访问资源的情况
 */
@Getter
public class UnauthorizedException extends RuntimeException {

    private final int code;

    public UnauthorizedException(String message) {
        super(message);
        this.code = ResultCode.FORBIDDEN;
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.FORBIDDEN;
    }

    public UnauthorizedException(int code, String message) {
        super(message);
        this.code = code;
    }

}
