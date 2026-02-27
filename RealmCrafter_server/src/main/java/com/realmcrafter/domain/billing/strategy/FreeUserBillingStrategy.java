package com.realmcrafter.domain.billing.strategy;

import com.realmcrafter.domain.billing.BillingResult;
import com.realmcrafter.domain.billing.InsufficientTokenException;
import com.realmcrafter.infrastructure.persistence.entity.UserDO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 免费用户计费策略：扣除代付 Token。
 */
@Component
@RequiredArgsConstructor
public class FreeUserBillingStrategy implements BillingStrategy {

    /** 每生成一章消耗的 Token 数，可配置 */
    @Value("${realmcrafter.billing.tokens-per-chapter:5000}")
    private long tokensPerChapter = 5000;

    @Override
    public boolean isByok() {
        return false;
    }

    @Override
    public BillingResult beforeChapterGeneration(UserDO user) {
        long balance = user.getTokenBalance() != null ? user.getTokenBalance() : 0;
        if (balance < tokensPerChapter) {
            throw new InsufficientTokenException("Token 不足，请观看激励视频或充值后再试");
        }

        user.setTokenBalance(balance - tokensPerChapter);
        return BillingResult.ok();
    }
}
