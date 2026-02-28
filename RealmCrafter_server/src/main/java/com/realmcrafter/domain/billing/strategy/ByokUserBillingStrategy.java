package com.realmcrafter.domain.billing.strategy;

import com.realmcrafter.domain.billing.AdTriggerRequiredException;
import com.realmcrafter.domain.billing.BillingResult;
import com.realmcrafter.infrastructure.persistence.entity.UserDO;
import com.realmcrafter.infrastructure.redis.AdWatchTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * BYOK 用户计费策略：不扣平台 Token，仅增加用户实体上的互动计数；
 * 满 10 章且无免广告特权时抛出 AdTriggerRequiredException，交由全局异常处理器返回 451。
 * 若用户已通过 /ad/callback 核销过广告，则本次不触发 451（消费 ad:watched 标记）。
 */
@Component
@RequiredArgsConstructor
public class ByokUserBillingStrategy implements BillingStrategy {

    private static final int AD_TRIGGER_INTERVAL = 10;

    private final AdWatchTokenService adWatchTokenService;

    @Override
    public boolean isByok() {
        return true;
    }

    @Override
    public BillingResult beforeChapterGeneration(UserDO user) {
        Integer counter = user.getInteractionCounter();
        if (counter == null) {
            counter = 0;
        }
        counter++;
        user.setInteractionCounter(counter);

        if (shouldTriggerAd(counter, user.getAdFreeExpireTime())) {
            // 若已通过 callback 标记“已观看”，则消费该标记并放行，不再抛 451
            if (adWatchTokenService.consumeAdWatched(user.getId())) {
                return BillingResult.ok();
            }
            throw new AdTriggerRequiredException();
        }

        return BillingResult.ok();
    }

    private static boolean shouldTriggerAd(long count, LocalDateTime adFreeExpireTime) {
        if (count % AD_TRIGGER_INTERVAL != 0) {
            return false;
        }
        if (adFreeExpireTime != null && adFreeExpireTime.isAfter(LocalDateTime.now())) {
            return false;
        }
        return true;
    }
}
