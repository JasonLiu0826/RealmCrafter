package com.realmcrafter.config;

/**
 * 流式输出滑动窗口审计命中违禁内容时抛出，由 Controller 捕获并切断 SSE。
 */
public class ContentViolationException extends RuntimeException {

    public ContentViolationException(String message) {
        super(message);
    }

    public ContentViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
