package com.realmcrafter.domain.billing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 计费/心跳处理结果：是否需触发广告、是否成功等。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BillingResult {

    /** 是否需触发前端广告（灵能补给插屏） */
    private boolean needAd;
    /** 是否允许继续（扣费成功或 BYOK 无需扣费） */
    private boolean success;
    private String message;

    public static BillingResult ok() {
        BillingResult r = new BillingResult();
        r.setNeedAd(false);
        r.setSuccess(true);
        return r;
    }

    public static BillingResult needAd() {
        BillingResult r = new BillingResult();
        r.setNeedAd(true);
        r.setSuccess(true);
        r.setMessage("AD_TRIGGER");
        return r;
    }
}
