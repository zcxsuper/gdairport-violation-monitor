package com.example.exception;

import com.example.domain.ResultCode;
import lombok.Getter;

/**
 * Token无效异常（401 Unauthorized）
 * 用于处理token验证失败、token过期等情况
 */
@Getter
public class TokenInvalidException extends RuntimeException {

    private final int code;

    public TokenInvalidException(String message) {
        super(message);
        this.code = ResultCode.UNAUTHORIZED;
    }

    public TokenInvalidException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.UNAUTHORIZED;
    }

    public TokenInvalidException(int code, String message) {
        super(message);
        this.code = code;
    }

}
