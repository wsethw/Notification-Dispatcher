package com.portfolio.notification.infrastructure;

import com.portfolio.notification.domain.NotificationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostgresNotificationRepository extends JpaRepository<NotificationAudit, Long> {
    NotificationAudit findByNotificationId(String notificationId);
}