package com.realmcrafter.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 使用 Redis 高速记录用户的生成交互次数（每生成 1 章 +1），
 * 用于判断是否触发广告（count % 10 == 0）。
 */
@Component
@RequiredArgsConstructor
public class AdCounterCache {

    private static final String KEY_PREFIX = "ad:counter:";
    private static final Duration TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;

    /**
     * 将用户的交互计数 +1，返回递增后的值。
     */
    public long increment(Long userId) {
        String key = KEY_PREFIX + userId;
        Long after = redisTemplate.opsForValue().increment(key);
        if (after != null && after == 1) {
            redisTemplate.expire(key, TTL);
        }
        return after != null ? after : 0;
    }

    /**
     * 获取当前交互计数值（未存在则 0）。
     */
    public long get(Long userId) {
        String key = KEY_PREFIX + userId;
        String val = redisTemplate.opsForValue().get(key);
        return val != null ? Long.parseLong(val, 10) : 0;
    }

    /**
     * 重置计数（如免广告期内或对账后）。
     */
    public void reset(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}
