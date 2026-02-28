package com.realmcrafter.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 广告观看一次性令牌与“已观看”状态。
 * 451 响应时签发 adToken；前端观看完成后调用 /ad/callback 携带 token，
 * 后端核销后设置 ad:watched，使下次心跳不再抛 451。
 */
@Service
@RequiredArgsConstructor
public class AdWatchTokenService {

    private static final String TOKEN_PREFIX = "ad:token:";
    private static final String WATCHED_PREFIX = "ad:watched:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);
    private static final Duration WATCHED_TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate redisTemplate;

    /**
     * 为当前需广告用户签发一次性令牌，供 451 响应体返回。
     */
    public String createToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, String.valueOf(userId), TOKEN_TTL);
        return token;
    }

    /**
     * 核销令牌：仅当 token 有效时返回对应用户 ID 并删除该 token。
     */
    public Long consumeToken(String token) {
        if (token == null || token.isBlank()) return null;
        String key = TOKEN_PREFIX + token;
        String userIdStr = redisTemplate.opsForValue().get(key);
        if (userIdStr == null) return null;
        redisTemplate.delete(key);
        try {
            return Long.parseLong(userIdStr, 10);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 标记用户已观看广告（由 callback 调用）。
     */
    public void setAdWatched(Long userId) {
        if (userId == null) return;
        redisTemplate.opsForValue().set(WATCHED_PREFIX + userId, "1", WATCHED_TTL);
    }

    /**
     * 消费“已观看”标记：若存在则删除并返回 true，使本次心跳不再触发 451。
     */
    public boolean consumeAdWatched(Long userId) {
        if (userId == null) return false;
        String key = WATCHED_PREFIX + userId;
        Boolean deleted = redisTemplate.delete(key);
        return Boolean.TRUE.equals(deleted);
    }
}
