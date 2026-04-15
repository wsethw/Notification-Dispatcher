package com.portfolio.notification.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisRateLimiter {
    private final RedisTemplate<String, String> redisTemplate;

    private static final int MAX_REQUESTS = 10; // 10 requisições
    private static final long WINDOW_SECONDS = 60; // Por minuto

    public boolean allowRequest(String clientId) {
        String key = "ratelimit:" + clientId;
        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount != null && currentCount == 1) {
            Duration expiration = Duration.ofSeconds(WINDOW_SECONDS);
            if (expiration != null) {
                redisTemplate.expire(key, expiration);
            }
        }

        return currentCount != null && currentCount <= MAX_REQUESTS;
    }

    public long getRemainingRequests(String clientId) {
        String key = "ratelimit:" + clientId;
        Object count = redisTemplate.opsForValue().get(key);
        if (count == null) return MAX_REQUESTS;
        long countValue = Long.parseLong(count.toString());
        return Math.max(0, MAX_REQUESTS - countValue);
    }
}