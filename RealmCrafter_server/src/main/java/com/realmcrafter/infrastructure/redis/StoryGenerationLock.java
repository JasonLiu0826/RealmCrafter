package com.realmcrafter.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis 的故事生成锁：同一故事包同时只允许一次生成，防止并发重复触发。
 */
@Component
@RequiredArgsConstructor
public class StoryGenerationLock {

    private static final String KEY_PREFIX = "engine:lock:story:";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    /**
     * 尝试获取锁，仅当当前故事无锁时成功。
     *
     * @param storyId 故事 ID
     * @return true 表示获取成功，false 表示已被占用
     */
    public boolean tryLock(String storyId) {
        String key = KEY_PREFIX + storyId;
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_TTL);
        return Boolean.TRUE.equals(ok);
    }

    /**
     * 释放锁。
     */
    public void unlock(String storyId) {
        redisTemplate.delete(KEY_PREFIX + storyId);
    }
}
