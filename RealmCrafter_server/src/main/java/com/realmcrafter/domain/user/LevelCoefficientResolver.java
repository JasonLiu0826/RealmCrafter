package com.realmcrafter.domain.user;

/**
 * 推荐/流量权重中的创作者等级乘数。
 * Lv1(1.0), Lv2(1.1), Lv3(1.2), Lv4(1.5), Lv5(2.0), Lv5金牌(2.5)。
 */
public final class LevelCoefficientResolver {

    private LevelCoefficientResolver() {}

    public static double trafficWeightCoefficient(int creatorLevel, boolean isGoldenCreator) {
        if (creatorLevel >= 5 && isGoldenCreator) {
            return 2.5;
        }
        switch (creatorLevel) {
            case 5: return 2.0;
            case 4: return 1.5;
            case 3: return 1.2;
            case 2: return 1.1;
            default: return 1.0;
        }
    }
}
