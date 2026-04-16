package com.portfolio.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.notification.application.SendNotificationUseCase;
import com.portfolio.notification.application.exception.RateLimitExceededException;
import com.portfolio.notification.controller.dto.SendNotificationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SendNotificationUseCase sendNotificationUseCase;

    @Test
    void shouldAcceptAValidNotification() throws Exception {
        when(sendNotificationUseCase.execute(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Map.of(
                        "notificationId", "notif-1",
                        "status", "PENDING",
                        "channel", "push"
                ));

        SendNotificationRequest request = new SendNotificationRequest("user-1", "push", "Hello", "World");

        mockMvc.perform(post("/api/v1/notifications/send")
                        .header("X-Client-Id", "client-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.notificationId").value("notif-1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldRejectInvalidPayload() throws Exception {
        SendNotificationRequest request = new SendNotificationRequest("user-1", "", "Hello", "World");

        mockMvc.perform(post("/api/v1/notifications/send")
                        .header("X-Client-Id", "client-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnTooManyRequestsWhenUseCaseRateLimits() throws Exception {
        when(sendNotificationUseCase.execute(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RateLimitExceededException("Rate limit exceeded for clientId: client-1"));

        SendNotificationRequest request = new SendNotificationRequest("user-1", "push", "Hello", "World");

        mockMvc.perform(post("/api/v1/notifications/send")
                        .header("X-Client-Id", "client-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value("RATE_LIMITED"));
    }

    @Test
    void shouldRequireClientHeader() throws Exception {
        SendNotificationRequest request = new SendNotificationRequest("user-1", "push", "Hello", "World");

        mockMvc.perform(post("/api/v1/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }
}