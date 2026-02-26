package com.realmcrafter.domain.billing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 当用户尝试使用 VIP 专属主题但未开通或已过期时抛出。
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class VipAccessDeniedException extends RuntimeException {

    public VipAccessDeniedException(String message) {
        super(message);
    }
}

