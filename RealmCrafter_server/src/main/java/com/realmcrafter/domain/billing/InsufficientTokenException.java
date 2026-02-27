package com.realmcrafter.domain.billing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 免费用户 Token 不足时抛出，需观看激励视频或充值后再生成章节。
 */
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class InsufficientTokenException extends RuntimeException {

    public InsufficientTokenException(String message) {
        super(message);
    }
}
