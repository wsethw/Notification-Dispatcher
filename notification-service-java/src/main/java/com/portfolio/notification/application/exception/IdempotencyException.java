package com.portfolio.notification.application.exception;

public class IdempotencyException extends RuntimeException {
    public IdempotencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
