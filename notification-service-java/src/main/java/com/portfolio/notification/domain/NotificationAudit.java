package com.portfolio.notification.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Entity
@Table(name = "notification_audit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String notificationId;

    @Column
    private String clientId;

    @Column
    private String userId;

    @Column
    private String channel;

    @Column(length = 500)
    private String subject;

    @Column(length = 2000)
    private String body;

    @Column
    private String idempotencyKey;

    @Column
    private String status;

    @Column
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime updatedAt;
}
