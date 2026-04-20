package com.portfolio.notification.controller;

import com.portfolio.notification.application.SendNotificationUseCase;
import com.portfolio.notification.controller.dto.SendNotificationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    private final SendNotificationUseCase sendNotificationUseCase;

    /**
     * Send a notification through the dispatcher.
     *
     * @param request          The notification request payload
     * @param clientId         Client identifier (required header)
     * @param idempotencyKey   Idempotency key for request deduplication (optional header)
     * @return ResponseEntity with notification details or error message
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request,
            @RequestHeader(value = "X-Client-Id") String clientId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        String normalizedClientId = clientId == null ? "" : clientId.trim();
        if (normalizedClientId.isBlank()) {
            throw new IllegalArgumentException("X-Client-Id header is required");
        }

        String normalizedIdempotencyKey = (idempotencyKey == null || idempotencyKey.isBlank())
                ? UUID.randomUUID().toString()
                : idempotencyKey.trim();

        logger.info("Processing notification for clientId: {}, userId: {}, channel: {}",
                normalizedClientId, request.userId(), request.channel());

        Map<String, Object> response = sendNotificationUseCase.execute(
                normalizedClientId,
                request.userId().trim(),
                request.channel().trim(),
                request.subject().trim(),
                request.body().trim(),
                normalizedIdempotencyKey
        );

        logger.info("Notification accepted successfully: {}", response.get("notificationId"));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Health check endpoint.
     *
     * @return Service health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        logger.debug("Health check requested");
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
