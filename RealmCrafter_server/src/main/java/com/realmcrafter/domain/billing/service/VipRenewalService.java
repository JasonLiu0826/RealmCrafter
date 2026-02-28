package com.realmcrafter.domain.billing.service;

import com.realmcrafter.domain.user.ExpAction;
import com.realmcrafter.domain.user.service.UserExpService;
import com.realmcrafter.infrastructure.persistence.entity.UserDO;
import com.realmcrafter.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * VIP 续期：更新到期时间并发放续期经验（含等级&lt;2 时强制跃迁 Lv2）。
 * 由支付回调或管理端在充值/续费成功后调用。
 */
@Service
@RequiredArgsConstructor
public class VipRenewalService {

    private final UserRepository userRepository;
    private final UserExpService userExpService;

    /**
     * 续期或开通 VIP：更新 vipExpireTime 并给用户加 VIP_RENEW 经验。
     */
    @Transactional
    public void renewVip(Long userId, LocalDateTime newExpireTime) {
        if (userId == null || newExpireTime == null) return;
        UserDO user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        user.setVipExpireTime(newExpireTime);
        userRepository.save(user);
        userExpService.addExp(userId, ExpAction.VIP_RENEW);
    }
}
