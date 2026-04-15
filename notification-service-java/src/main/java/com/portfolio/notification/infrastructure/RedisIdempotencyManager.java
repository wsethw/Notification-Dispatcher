package com.portfolio.notification.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisIdempotencyManager {
    private final RedisTemplate<String, String> redisTemplate;

    private static final long TTL_HOURS = 24;

    public boolean isProcessed(String clientId, String idempotencyKey) {
        String key = "idempotency:" + clientId + ":" + idempotencyKey;
        Boolean hasKey = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(hasKey);
    }

    @SuppressWarnings("null")
    public void markAsProcessed(String clientId, String idempotencyKey, String resultJson) {
        String key = "idempotency:" + clientId + ":" + idempotencyKey;
        if (resultJson != null) {
            redisTemplate.opsForValue().set(key, resultJson, Duration.ofHours(TTL_HOURS));
        }
    }

    public String getProcessedResult(String clientId, String idempotencyKey) {
        String key = "idempotency:" + clientId + ":" + idempotencyKey;
        Object result = redisTemplate.opsForValue().get(key);
        return result != null ? result.toString() : null;
    }
}