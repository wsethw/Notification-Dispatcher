package com.portfolio.notification.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.notification.application.exception.RateLimitExceededException;
import com.portfolio.notification.domain.NotificationAudit;
import com.portfolio.notification.infrastructure.PostgresNotificationRepository;
import com.portfolio.notification.infrastructure.RedisIdempotencyManager;
import com.portfolio.notification.infrastructure.RedisPublisher;
import com.portfolio.notification.infrastructure.RedisRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendNotificationUseCaseTest {

    @Mock
    private RedisRateLimiter rateLimiter;

    @Mock
    private RedisIdempotencyManager idempotencyManager;

    @Mock
    private RedisPublisher redisPublisher;

    @Mock
    private PostgresNotificationRepository repository;

    private SendNotificationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SendNotificationUseCase(
                rateLimiter,
                idempotencyManager,
                redisPublisher,
                repository,
                new ObjectMapper()
        );
    }

    @Test
    void shouldReturnCachedResultWhenIdempotencyKeyWasAlreadyProcessed() {
        when(rateLimiter.allowRequest("client-1")).thenReturn(true);
        when(idempotencyManager.isProcessed("client-1", "idem-1")).thenReturn(true);
        when(idempotencyManager.getProcessedResult("client-1", "idem-1"))
                .thenReturn("{\"notificationId\":\"cached-1\",\"status\":\"PENDING\"}");

        Map<String, Object> response = useCase.execute(
                "client-1",
                "user-1",
                "push",
                "Subject",
                "Body",
                "idem-1"
        );

        assertEquals("cached-1", response.get("notificationId"));
        verify(repository, never()).save(any());
        verify(redisPublisher, never()).publishNotification(any(), anyMap());
    }

    @Test
    void shouldFailFastWhenRateLimitIsExceeded() {
        when(rateLimiter.allowRequest("client-1")).thenReturn(false);

        assertThrows(
                RateLimitExceededException.class,
                () -> useCase.execute("client-1", "user-1", "push", "Subject", "Body", "idem-1")
        );
    }

    @Test
    void shouldRejectUnknownChannels() {
        when(rateLimiter.allowRequest("client-1")).thenReturn(true);
        when(idempotencyManager.isProcessed("client-1", "idem-1")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute("client-1", "user-1", "fax", "Subject", "Body", "idem-1")
        );

        assertEquals("channel must be one of: push, email, sms", exception.getMessage());
    }

    @Test
    void shouldPersistPublishAndCacheAValidNotification() {
        when(rateLimiter.allowRequest("client-1")).thenReturn(true);
        when(idempotencyManager.isProcessed("client-1", "idem-1")).thenReturn(false);

        Map<String, Object> response = useCase.execute(
                "client-1",
                "user-1",
                "push",
                "Subject",
                "Body",
                "idem-1"
        );

        ArgumentCaptor<NotificationAudit> auditCaptor = ArgumentCaptor.forClass(NotificationAudit.class);
        verify(repository).save(auditCaptor.capture());
        verify(redisPublisher).publishNotification(eq("push"), anyMap());
        verify(idempotencyManager).markAsProcessed(eq("client-1"), eq("idem-1"), any(String.class));

        NotificationAudit persistedAudit = auditCaptor.getValue();
        assertEquals("client-1", persistedAudit.getClientId());
        assertEquals("push", persistedAudit.getChannel());
        assertTrue(response.containsKey("notificationId"));
        assertEquals("PENDING", response.get("status"));
    }
}