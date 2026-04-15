package com.portfolio.notification.application;

import com.portfolio.notification.domain.NotificationAudit;
import com.portfolio.notification.domain.NotificationDomain;
import com.portfolio.notification.infrastructure.PostgresNotificationRepository;
import com.portfolio.notification.infrastructure.RedisIdempotencyManager;
import com.portfolio.notification.infrastructure.RedisPublisher;
import com.portfolio.notification.infrastructure.RedisRateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings({ "unchecked", "null" })
public class SendNotificationUseCase {
    private final RedisRateLimiter rateLimiter;
    private final RedisIdempotencyManager idempotencyManager;
    private final RedisPublisher redisPublisher;
    private final PostgresNotificationRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> execute(
            String clientId,
            String userId,
            String channel,
            String subject,
            String body,
            String idempotencyKey) {

        // 1. Rate Limiting
        if (!rateLimiter.allowRequest(clientId)) {
            throw new RuntimeException("Rate limit exceeded for clientId: " + clientId);
        }

        // 2. Idempotency Check
        if (idempotencyManager.isProcessed(clientId, idempotencyKey)) {
            String cachedResult = idempotencyManager.getProcessedResult(clientId, idempotencyKey);
            if (cachedResult != null) {
                try {
                    return objectMapper.readValue(cachedResult, Map.class);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse cached result", e);
                }
            }
        }

        // 3. Generate Notification ID
        String notificationId = UUID.randomUUID().toString();

        // 4. Persist Audit (Status: PENDING)
        NotificationAudit audit = NotificationAudit.builder()
                .notificationId(notificationId)
                .clientId(clientId)
                .userId(userId)
                .channel(channel)
                .subject(subject)
                .body(body)
                .idempotencyKey(idempotencyKey)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        try {
            repository.save(audit);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist notification audit", e);
        }

        // 5. Build Domain Object for Publishing
        NotificationDomain domain = NotificationDomain.builder()
                .notificationId(notificationId)
                .clientId(clientId)
                .userId(userId)
                .channel(channel)
                .subject(subject)
                .body(body)
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .status("PENDING")
                .build();

        // 6. Convert to Map and Publish to Redis
        Map<String, Object> domainMap = objectMapper.convertValue(domain, Map.class);
        try {
            redisPublisher.publishNotification(channel, domainMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish notification to Redis", e);
        }

        // 7. Build Response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("notificationId", notificationId);
        response.put("clientId", clientId);
        response.put("channel", channel);
        response.put("status", "PENDING");
        response.put("createdAt", LocalDateTime.now());

        // 8. Cache Result for Idempotency
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            idempotencyManager.markAsProcessed(clientId, idempotencyKey, responseJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to cache idempotency result", e);
        }

        return response;
    }
}