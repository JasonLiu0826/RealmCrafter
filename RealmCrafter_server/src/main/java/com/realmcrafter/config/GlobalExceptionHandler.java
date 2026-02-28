package com.realmcrafter.config;

import com.realmcrafter.api.dto.Result;
import com.realmcrafter.domain.billing.AdTriggerRequiredException;
import com.realmcrafter.domain.billing.InsufficientTokenException;
import com.realmcrafter.infrastructure.redis.AdWatchTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器。
 * 拦截业务异常与校验异常，统一封装为 Result 返回。
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    /** 同步冲突业务码，便于前端识别并展示合并面板 */
    public static final int CODE_SYNC_CONFLICT = 409;

    /** 广告触发业务码 451，前端据此调起插屏 */
    public static final int CODE_AD_TRIGGER = 451;

    private final AdWatchTokenService adWatchTokenService;

    @ExceptionHandler(AdTriggerRequiredException.class)
    public ResponseEntity<Result<Map<String, Object>>> handleAdTrigger(AdTriggerRequiredException e, HttpServletRequest request) {
        log.debug("AD_TRIGGER: uri={}", request.getRequestURI());
        Map<String, Object> data = new HashMap<>();
        data.put("needAd", true);
        Long userId = resolveCurrentUserId();
        if (userId != null) {
            String adToken = adWatchTokenService.createToken(userId);
            data.put("adToken", adToken);
        }
        Result<Map<String, Object>> body = Result.<Map<String, Object>>builder()
                .code(CODE_AD_TRIGGER)
                .message("AD_TRIGGER")
                .data(data)
                .build();
        return ResponseEntity.status(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS).body(body);
    }

    private static Long resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Long) return (Long) principal;
        if (principal instanceof String) {
            try { return Long.parseLong((String) principal); } catch (NumberFormatException ignored) { return null; }
        }
        try {
            Object id = principal.getClass().getMethod("getId").invoke(principal);
            return id instanceof Long ? (Long) id : null;
        } catch (Exception ignored) { return null; }
    }

    @ExceptionHandler(SyncConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<?> handleSyncConflict(SyncConflictException e, HttpServletRequest request) {
        log.warn("Sync conflict: uri={}, message={}", request.getRequestURI(), e.getMessage());
        return Result.fail(CODE_SYNC_CONFLICT, e.getMessage());
    }

    @ExceptionHandler(InsufficientTokenException.class)
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public Result<?> handleInsufficientToken(InsufficientTokenException e, HttpServletRequest request) {
        log.warn("Insufficient token: uri={}, message={}", request.getRequestURI(), e.getMessage());
        return Result.fail(402, e.getMessage());
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
