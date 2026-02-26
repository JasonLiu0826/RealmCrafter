package com.realmcrafter.domain.billing.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 与 VIP 相关的领域校验逻辑。
 */
@Service
public class VipValidator {

    /**
     * 判断当前用户是否拥有有效的 VIP 资格。
     *
     * @param vipExpireTime VIP 到期时间，可能为 null
     * @return true 表示 VIP 仍在有效期内
     */
    public boolean hasActiveVip(LocalDateTime vipExpireTime) {
        return vipExpireTime != null && vipExpireTime.isAfter(LocalDateTime.now());
    }
}

