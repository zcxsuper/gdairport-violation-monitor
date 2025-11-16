package com.example.advice;

import com.example.domain.ResponseResult;
import com.example.domain.ResultCode;
import com.example.exception.BadRequestException;
import com.example.exception.FileStorageException;
import com.example.exception.NotFoundException;
import com.example.exception.TokenInvalidException;
import com.example.exception.UnauthorizedException;
import com.example.exception.UserException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理所有异常，返回规范的ResponseResult格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数验证异常（@Valid @RequestBody）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseResult<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("参数验证失败: {}", e.getMessage());
        
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        return ResponseResult.error(ResultCode.BAD_REQUEST, "参数验证失败: " + errorMessage);
    }

    /**
     * 处理参数绑定异常（@Valid @ModelAttribute）
     */
    @ExceptionHandler(BindException.class)
    public ResponseResult<Object> handleBindException(BindException e) {
        log.warn("参数绑定失败: {}", e.getMessage());
        
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        return ResponseResult.error(ResultCode.BAD_REQUEST, "参数绑定失败: " + errorMessage);
    }

    /**
     * 处理约束违反异常（@Validated）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseResult<Object> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("约束验证失败: {}", e.getMessage());
        
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String errorMessage = violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        
        return ResponseResult.error(ResultCode.BAD_REQUEST, "参数验证失败: " + errorMessage);
    }

    /**
     * 处理BadRequestException（400 请求错误）
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseResult<Object> handleBadRequestException(BadRequestException e) {
        log.warn("请求错误: {}", e.getMessage());
        return ResponseResult.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理TokenInvalidException（401 未授权）
     */
    @ExceptionHandler(TokenInvalidException.class)
    public ResponseResult<Object> handleTokenInvalidException(TokenInvalidException e) {
        log.warn("Token验证失败: {}", e.getMessage());
        return ResponseResult.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理UnauthorizedException（403 禁止访问）
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseResult<Object> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("无权限访问: {}", e.getMessage());
        return ResponseResult.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理UserException（400 用户相关错误）
     */
    @ExceptionHandler(UserException.class)
    public ResponseResult<Object> handleUserException(UserException e) {
        log.warn("用户操作错误: {}", e.getMessage());
        return ResponseResult.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理NotFoundException（404 资源未找到）
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseResult<Object> handleNotFoundException(NotFoundException e) {
        log.warn("资源未找到: {}", e.getMessage());
        return ResponseResult.error(ResultCode.NOT_FOUND, e.getMessage());
    }

    /**
     * 处理FileStorageException（500 文件存储错误）
     */
    @ExceptionHandler(FileStorageException.class)
    public ResponseResult<Object> handleFileStorageException(FileStorageException e) {
        log.error("文件操作失败: {}", e.getMessage(), e);
        return ResponseResult.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理所有其他未捕获的异常（500 服务器内部错误）
     */
    @ExceptionHandler(Exception.class)
    public ResponseResult<Object> handleException(Exception e) {
        log.error("服务器内部错误: {}", e.getMessage(), e);
        // 生产环境建议不返回详细的异常信息，仅返回通用错误信息
        String message = "服务器内部错误，请联系管理员";
        // 开发环境可以返回详细错误信息
        // String message = "服务器内部错误: " + e.getMessage();
        return ResponseResult.error(ResultCode.SERVER_ERROR, message);
    }
}
