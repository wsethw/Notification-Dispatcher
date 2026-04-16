package com.portfolio.notification.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisRateLimiter {
    private final RedisTemplate<String, String> redisTemplate;
    @Value("${notification.rate-limit.max-requests:10}")
    private int maxRequests;
    @Value("${notification.rate-limit.window-seconds:60}")
    private long windowSeconds;

    public boolean allowRequest(String clientId) {
        String key = "ratelimit:" + clientId;
        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount != null && currentCount == 1) {
            Duration expiration = Duration.ofSeconds(windowSeconds);
            if (expiration != null) {
                redisTemplate.expire(key, expiration);
            }
        }

        return currentCount != null && currentCount <= maxRequests;
    }

    public long getRemainingRequests(String clientId) {
        String key = "ratelimit:" + clientId;
        Object count = redisTemplate.opsForValue().get(key);
        if (count == null) return maxRequests;
        long countValue = Long.parseLong(count.toString());
        return Math.max(0, maxRequests - countValue);
    }
}
