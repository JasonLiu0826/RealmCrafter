package com.realmcrafter.domain.billing;

/**
 * 当互动计数满 N 章且用户无免广告特权时抛出，
 * 由 GlobalExceptionHandler 捕获并返回 451，触发前端展示广告。
 */
public class AdTriggerRequiredException extends RuntimeException {

    public static final int STATUS_CODE = 451;

    public AdTriggerRequiredException() {
        super("AD_TRIGGER");
    }

    public AdTriggerRequiredException(String message) {
        super(message);
    }
}
