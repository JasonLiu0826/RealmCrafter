package com.realmcrafter.config;

/**
 * 多端同步冲突异常。
 * 当客户端提交的 version_id 与服务器当前版本不一致时抛出，
 * 前端拦截后展示左右双栏合并面板。
 */
public class SyncConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SyncConflictException() {
        super("Sync conflict: local version is outdated, please merge or refresh.");
    }

    public SyncConflictException(String message) {
        super(message);
    }

    public SyncConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
