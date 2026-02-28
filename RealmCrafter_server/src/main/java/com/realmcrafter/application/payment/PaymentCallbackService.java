package com.realmcrafter.application.payment;

import com.realmcrafter.domain.billing.service.VipRenewalService;
import com.realmcrafter.infrastructure.persistence.entity.UserDO;
import com.realmcrafter.infrastructure.persistence.entity.WalletTransactionDO;
import com.realmcrafter.infrastructure.persistence.repository.UserRepository;
import com.realmcrafter.infrastructure.persistence.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 支付回调落地：加水晶、续 VIP，均以外部订单号幂等。
 */
@Service
@RequiredArgsConstructor
public class PaymentCallbackService {

    private final UserRepository userRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final VipRenewalService vipRenewalService;

    /**
     * 充值水晶：幂等（同一 externalOrderId 只入账一次）。
     */
    @Transactional
    public Optional<String> rechargeCrystal(Long userId, BigDecimal amount, String externalOrderId) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.of("参数无效");
        }
        if (externalOrderId != null && !externalOrderId.isBlank()
                && walletTransactionRepository.findByExternalOrderId(externalOrderId).isPresent()) {
            return Optional.empty(); // 已处理过，视为成功
        }
        UserDO user = userRepository.findById(userId).orElse(null);
        if (user == null) return Optional.of("用户不存在");
        user.setCrystalBalance(user.getCrystalBalance().add(amount));
        userRepository.save(user);
        WalletTransactionDO tx = new WalletTransactionDO();
        tx.setUserId(userId);
        tx.setAmount(amount);
        tx.setType(WalletTransactionDO.Type.RECHARGE);
        tx.setDescription("充值");
        tx.setExternalOrderId(externalOrderId);
        walletTransactionRepository.save(tx);
        return Optional.empty();
    }

    /**
     * 续期 VIP：在现有到期时间上叠加天数（已过期则从当前时间起算）。
     */
    @Transactional
    public Optional<String> renewVipDays(Long userId, int daysToAdd) {
        if (userId == null || daysToAdd <= 0) return Optional.of("参数无效");
        UserDO user = userRepository.findById(userId).orElse(null);
        if (user == null) return Optional.of("用户不存在");
        LocalDateTime base = user.getVipExpireTime();
        if (base == null || base.isBefore(LocalDateTime.now())) {
            base = LocalDateTime.now();
        }
        LocalDateTime newExpire = base.plusDays(daysToAdd);
        vipRenewalService.renewVip(userId, newExpire);
        return Optional.empty();
    }
}
