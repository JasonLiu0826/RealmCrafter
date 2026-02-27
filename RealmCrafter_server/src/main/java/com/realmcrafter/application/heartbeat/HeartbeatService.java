package com.realmcrafter.application.heartbeat;

import com.realmcrafter.domain.billing.strategy.BillingStrategy;
import com.realmcrafter.infrastructure.persistence.entity.UserDO;
import com.realmcrafter.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 心跳/生成前检查：根据用户类型选择计费策略，更新互动计数，
 * 若满足 count % 10 == 0 且无免广告则抛出 AD_TRIGGER 供前端展示广告。
 */
@Service
@RequiredArgsConstructor
public class HeartbeatService {

    private final UserRepository userRepository;
    private final List<BillingStrategy> billingStrategies;

    @Transactional
    public void heartbeat() {
        Long userId = resolveCurrentUserId();
        UserDO user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        BillingStrategy strategy = resolveStrategy(user);
        // 计费策略内部负责扣费 / 递增互动计数，以及在满足条件时抛出 AdTriggerRequiredException。
        strategy.beforeChapterGeneration(user);

        // 心跳结束后统一持久化用户状态（无论是否 BYOK）。
        userRepository.save(user);
    }

    private BillingStrategy resolveStrategy(UserDO user) {
        boolean byok = Boolean.TRUE.equals(user.getIsByok());
        return billingStrategies.stream()
                .filter(s -> s.isByok() == byok)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到匹配的计费策略"));
    }

    /**
     * 从 Spring Security 上下文解析当前登录用户 ID。
     * <p>
     * 约定：认证成功后 Authentication#getPrincipal() 至少能提供一个可转为 Long 的用户标识，
     * 如 Long/字符串 userId，或自定义 Principal 对象中暴露的 getId()。
     */
    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("未认证用户，无法获取心跳上下文");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException ex) {
                throw new IllegalStateException("无法从字符串 Principal 解析用户 ID", ex);
            }
        }

        try {
            return (Long) principal.getClass().getMethod("getId").invoke(principal);
        } catch (Exception ignored) {
        }

        throw new IllegalStateException("无法从认证信息中解析当前用户 ID");
    }
}
