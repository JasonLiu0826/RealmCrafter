package com.realmcrafter.domain.billing.strategy;

import com.realmcrafter.domain.billing.BillingResult;
import com.realmcrafter.infrastructure.persistence.entity.UserDO;

/**
 * 商业化计费策略：章节生成前的扣费/计数与广告触发判断。
 * FreeUser：扣代付 Token；
 * ByokUser：不扣 Token，但参与互动计数，满 N 章触发广告。
 */
public interface BillingStrategy {

    /**
     * 是否为 BYOK 用户（不扣平台 Token）。
     */
    boolean isByok();

    /**
     * 章节生成前执行：扣费或仅计数。
     * 若需触发广告（如 count % 10 == 0 且无免广告），抛出 AdTriggerRequiredException。
     *
     * @param user 当前用户
     * @return 扣费/计数后的结果，包含是否需触发广告
     */
    BillingResult beforeChapterGeneration(UserDO user);
}
