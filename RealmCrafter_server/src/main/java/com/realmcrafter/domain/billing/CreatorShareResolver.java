package com.realmcrafter.domain.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 根据创作者等级与金牌标志解析分润比例。
 * Lv1/Lv2: 70%, Lv3: 75%, Lv4/Lv5(非金牌): 80%, Lv5(金牌): 90%。
 */
public final class CreatorShareResolver {

    private CreatorShareResolver() {}

    public static BigDecimal authorShareRatio(int creatorLevel, boolean isGoldenCreator) {
        if (creatorLevel >= 5 && isGoldenCreator) {
            return new BigDecimal("0.90");
        }
        if (creatorLevel >= 4) {
            return new BigDecimal("0.80");
        }
        if (creatorLevel >= 3) {
            return new BigDecimal("0.75");
        }
        return new BigDecimal("0.70");
    }
}
