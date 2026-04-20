package com.portfolio.notification.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.notification.application.exception.IdempotencyException;
import com.portfolio.notification.application.exception.NotificationDispatchException;
import com.portfolio.notification.application.exception.RateLimitExceededException;
import com.portfolio.notification.domain.NotificationAudit;
import com.portfolio.notification.domain.NotificationChannel;
import com.portfolio.notification.domain.NotificationDomain;
import com.portfolio.notification.infrastructure.PostgresNotificationRepository;
import com.portfolio.notification.infrastructure.RedisIdempotencyManager;
import com.portfolio.notification.infrastructure.RedisPublisher;
import com.portfolio.notification.infrastructure.RedisRateLimiter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings({ "unchecked", "null" })
public class SendNotificationUseCase {
    private static final Logger logger = LoggerFactory.getLogger(SendNotificationUseCase.class);
    private final RedisRateLimiter rateLimiter;
    private final RedisIdempotencyManager idempotencyManager;
    private final RedisPublisher redisPublisher;
    private final PostgresNotificationRepository repository;
    private final ObjectMapper objectMapper;

    public Map<String, Object> execute(
            String clientId,
            String userId,
            String channel,
            String subject,
            String body,
            String idempotencyKey) {
        Map<String, Object> cachedResponse = getCachedResponse(clientId, idempotencyKey);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        NotificationChannel notificationChannel = NotificationChannel.from(channel);
        if (!rateLimiter.allowRequest(clientId)) {
            throw new RateLimitExceededException("Rate limit exceeded for clientId: " + clientId);
        }

        String notificationId = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        NotificationAudit audit = NotificationAudit.builder()
                .notificationId(notificationId)
                .clientId(clientId)
                .userId(userId)
                .channel(notificationChannel.value())
                .subject(subject)
                .body(body)
                .idempotencyKey(idempotencyKey)
                .status("PENDING")
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            repository.save(audit);
        } catch (Exception e) {
            throw new NotificationDispatchException("Failed to persist notification audit", e);
        }

        NotificationDomain domain = NotificationDomain.builder()
                .notificationId(notificationId)
                .clientId(clientId)
                .userId(userId)
                .channel(notificationChannel.value())
                .subject(subject)
                .body(body)
                .idempotencyKey(idempotencyKey)
                .createdAt(now)
                .status("PENDING")
                .build();

        Map<String, Object> domainMap = objectMapper.convertValue(domain, Map.class);
        try {
            redisPublisher.publishNotification(notificationChannel.value(), domainMap);
        } catch (Exception e) {
            audit.setStatus("FAILED");
            audit.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            repository.save(audit);
            throw new NotificationDispatchException("Failed to publish notification to Redis", e);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("notificationId", notificationId);
        response.put("clientId", clientId);
        response.put("channel", notificationChannel.value());
        response.put("status", "PENDING");
        response.put("createdAt", now);

        try {
            String responseJson = objectMapper.writeValueAsString(response);
            idempotencyManager.markAsProcessed(clientId, idempotencyKey, responseJson);
        } catch (Exception e) {
            logger.error("Failed to cache idempotency result for notification {}", notificationId, e);
        }

        return response;
    }

    private Map<String, Object> getCachedResponse(String clientId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        String cachedResult = idempotencyManager.getProcessedResult(clientId, idempotencyKey);
        if (cachedResult == null) {
            return null;
        }

        try {
            return objectMapper.readValue(cachedResult, Map.class);
        } catch (Exception e) {
            throw new IdempotencyException("Failed to parse cached result", e);
        }
    }
}
