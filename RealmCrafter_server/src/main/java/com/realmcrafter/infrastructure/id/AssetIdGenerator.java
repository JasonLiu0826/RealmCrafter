package com.realmcrafter.infrastructure.id;

import java.util.UUID;

/**
 * 资产 ID 生成器。
 * 规则：前缀(sd/gs) + 用户ID后四位 + 8位随机字符(UUID 截取)。
 */
public final class AssetIdGenerator {

    private AssetIdGenerator() {
    }

    /**
     * 生成资产 ID。
     *
     * @param prefix sd 或 gs
     * @param userId 用户 ID
     * @return 形如 sd1002a1b2c3d4 的主键
     */
    public static String generateId(String prefix, Long userId) {
        if (prefix == null || prefix.isEmpty()) {
            throw new IllegalArgumentException("prefix 不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        String uidStr = String.valueOf(userId);
        String last4Uid = uidStr.substring(Math.max(0, uidStr.length() - 4));
        String random8 = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return prefix + last4Uid + random8;
    }
}

