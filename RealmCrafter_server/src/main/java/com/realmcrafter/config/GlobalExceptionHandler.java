package com.realmcrafter.config;

import com.realmcrafter.api.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器。
 * 拦截业务异常与校验异常，统一封装为 Result 返回。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 同步冲突业务码，便于前端识别并展示合并面板 */
    public static final int CODE_SYNC_CONFLICT = 409;

    @ExceptionHandler(SyncConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<?> handleSyncConflict(SyncConflictException e, HttpServletRequest request) {
        log.warn("Sync conflict: uri={}, message={}", request.getRequestURI(), e.getMessage());
        return Result.fail(CODE_SYNC_CONFLICT, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("Bad request: uri={}, message={}", request.getRequestURI(), e.getMessage());
        return Result.fail(400, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        log.warn("Validation failed: uri={}, message={}", request.getRequestURI(), message);
        return Result.fail(400, message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Binding validation failed");
        log.warn("Bind error: uri={}, message={}", request.getRequestURI(), message);
        return Result.fail(400, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception: uri={}", request.getRequestURI(), e);
        return Result.fail(500, "Internal server error");
    }
}
