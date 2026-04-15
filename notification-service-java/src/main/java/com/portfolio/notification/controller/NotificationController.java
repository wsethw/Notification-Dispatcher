package com.portfolio.notification.controller;

import com.portfolio.notification.application.SendNotificationUseCase;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
            @RequestBody SendNotificationRequest request,
            @RequestHeader(value = "X-Client-Id") String clientId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // Validate input
        if (clientId == null || clientId.isBlank()) {
            logger.warn("❌ Missing X-Client-Id header");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "X-Client-Id header is required"));
        }

        if (request == null) {
            logger.warn("❌ Missing request body");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Request body is required"));
        }

        if (request.getUserId() == null || request.getUserId().isBlank()) {
            logger.warn("❌ Missing userId in request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "userId is required"));
        }

        if (request.getChannel() == null || request.getChannel().isBlank()) {
            logger.warn("❌ Missing channel in request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "channel is required (push, email, or sms)"));
        }

        if (request.getSubject() == null || request.getSubject().isBlank()) {
            logger.warn("❌ Missing subject in request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "subject is required"));
        }

        if (request.getBody() == null || request.getBody().isBlank()) {
            logger.warn("❌ Missing body in request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "body is required"));
        }

        // Generate idempotency key if not provided
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = UUID.randomUUID().toString();
        }

        try {
            logger.info("📨 Processing notification for clientId: {}, userId: {}, channel: {}",
                    clientId, request.getUserId(), request.getChannel());

            Map<String, Object> response = sendNotificationUseCase.execute(
                    clientId,
                    request.getUserId(),
                    request.getChannel(),
                    request.getSubject(),
                    request.getBody(),
                    idempotencyKey
            );

            logger.info("✅ Notification sent successfully: {}", response.get("notificationId"));
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (RuntimeException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";

            if (errorMessage.contains("Rate limit")) {
                logger.warn("⚠️  Rate limit exceeded for clientId: {}", clientId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", errorMessage);
                errorResponse.put("status", "RATE_LIMITED");
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
            }

            if (errorMessage.contains("Idempotency") || errorMessage.contains("cache")) {
                logger.error("❌ Idempotency error: {}", errorMessage);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", errorMessage);
                errorResponse.put("status", "IDEMPOTENCY_ERROR");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

            logger.error("❌ Unexpected error processing notification: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", errorMessage);
            errorResponse.put("status", "INTERNAL_SERVER_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Health check endpoint.
     *
     * @return Service health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        logger.debug("🏥 Health check requested");
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    /**
     * Request DTO for sending notifications.
     */
    public static class SendNotificationRequest {
        @JsonProperty("userId")
        public String userId;

        @JsonProperty("channel")
        public String channel;

        @JsonProperty("subject")
        public String subject;

        @JsonProperty("body")
        public String body;

        // Constructors
        public SendNotificationRequest() {
        }

        public SendNotificationRequest(String userId, String channel, String subject, String body) {
            this.userId = userId;
            this.channel = channel;
            this.subject = subject;
            this.body = body;
        }

        // Getters and Setters
        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        @Override
        public String toString() {
            return "SendNotificationRequest{" +
                    "userId='" + userId + '\'' +
                    ", channel='" + channel + '\'' +
                    ", subject='" + subject + '\'' +
                    ", body='" + body + '\'' +
                    '}';
        }
    }
}