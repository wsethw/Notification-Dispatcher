package com.portfolio.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class NotificationDomain {
    private String notificationId;
    private String clientId;
    private String userId;
    private String channel; // email, sms, push
    private String subject;
    private String body;
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private String status; // PENDING, SENT, FAILED
}