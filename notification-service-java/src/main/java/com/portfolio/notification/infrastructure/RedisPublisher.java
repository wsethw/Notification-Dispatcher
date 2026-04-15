package com.portfolio.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RedisPublisher {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void publishNotification(String channel, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            String channelName = "channel:" + channel + ":request";
            if (json != null) {
                redisTemplate.convertAndSend(channelName, json);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish to Redis", e);
        }
    }
}