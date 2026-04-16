package com.portfolio.notification.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record SendNotificationRequest(
        @NotBlank(message = "userId is required") String userId,
        @NotBlank(message = "channel is required") String channel,
        @NotBlank(message = "subject is required") String subject,
        @NotBlank(message = "body is required") String body) {
}
