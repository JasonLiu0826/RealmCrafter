package com.realmcrafter.domain.billing;

import java.math.BigDecimal;

/**
 * 根据创作者等级校验资产定价上限。
 * Lv1~Lv3: 最大 15；Lv4/Lv5: 最大 20；Lv5 且金牌: 暂不设上限。
 */
public final class CreatorPriceValidator {

    public static final BigDecimal MAX_PRICE_LEVEL_1_3 = new BigDecimal("15");
    public static final BigDecimal MAX_PRICE_LEVEL_4_5 = new BigDecimal("20");

    private CreatorPriceValidator() {}

    /**
     * 校验价格是否在当前创作者等级允许范围内，超限则抛出 IllegalArgumentException。
     */
    public static void validatePrice(int creatorLevel, boolean isGoldenCreator, BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            return;
        }
        BigDecimal max;
        if (creatorLevel >= 5 && isGoldenCreator) {
            return;
        }
        if (creatorLevel >= 4) {
            max = MAX_PRICE_LEVEL_4_5;
        } else {
            max = MAX_PRICE_LEVEL_1_3;
        }
        if (price.compareTo(max) > 0) {
            throw new IllegalArgumentException("当前创作者等级不足以设置该价格");
        }
    }
}
