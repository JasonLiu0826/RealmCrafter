package com.realmcrafter.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 全局统一 API 响应封装。
 * 包含 code、message、data，便于前端统一处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码，0 表示成功 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 业务数据 */
    private T data;

    public static <T> Result<T> ok() {
        return Result.<T>builder()
                .code(0)
                .message("success")
                .data(null)
                .build();
    }

    public static <T> Result<T> ok(T data) {
        return Result.<T>builder()
                .code(0)
                .message("success")
                .data(data)
                .build();
    }

    public static <T> Result<T> ok(String message, T data) {
        return Result.<T>builder()
                .code(0)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> Result<T> fail(int code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }

    public static <T> Result<T> fail(String message) {
        return fail(-1, message);
    }
}
