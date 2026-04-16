package com.portfolio.notification.domain;

import java.util.Arrays;

public enum NotificationChannel {
    PUSH("push"),
    EMAIL("email"),
    SMS("sms");

    private final String value;

    NotificationChannel(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static NotificationChannel from(String rawValue) {
        return Arrays.stream(values())
                .filter(channel -> channel.value.equalsIgnoreCase(rawValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("channel must be one of: push, email, sms"));
    }
}
