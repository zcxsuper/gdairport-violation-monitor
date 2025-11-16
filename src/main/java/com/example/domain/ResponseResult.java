package com.example.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * controller返回给前端的数据
 * @param <T> 返回的数据对象类型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseResult<T> {

    private int code;       // 请求状态码,使用ResuleCode类的定义
    private String msg;     // 请求消息
    private T data;         // 返回的数据

    /**
     * 操作成功,并返回数据
     * @param data 要返回的数据
     */
    public static <T> ResponseResult<T> success(T data) {
        return new ResponseResult<>(ResultCode.SUCCESS, "操作成功", data);
    }

    /**
     * 操作成功
     */
    public static <T> ResponseResult<T> success() {
        return new ResponseResult<>(ResultCode.SUCCESS, "操作成功", null);
    }

    /**
     * 操作失败,返回指定的状态码和消息
     * @param code 状态码
     * @param msg 消息
     */
    public static <T> ResponseResult<T> error(int code, String msg) {
        return new ResponseResult<>(code, msg, null);
    }

    /**
     * 操作失败，返回一个异常
     * @param cause 异常
     */
    public static <T> ResponseResult<T> error(int code, Throwable cause) {
        return new ResponseResult<>(code, cause.getMessage(), null);
    }
}
