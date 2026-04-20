package com.portfolio.notification.controller;

import com.portfolio.notification.application.exception.IdempotencyException;
import com.portfolio.notification.application.exception.NotificationDispatchException;
import com.portfolio.notification.application.exception.RateLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class NotificationControllerAdvice {
    private static final Logger logger = LoggerFactory.getLogger(NotificationControllerAdvice.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Invalid request payload";
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(MissingRequestHeaderException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getHeaderName() + " header is required");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExceededException exception) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", exception.getMessage());
    }

    @ExceptionHandler(IdempotencyException.class)
    public ResponseEntity<Map<String, Object>> handleIdempotency(IdempotencyException exception) {
        return buildResponse(HttpStatus.CONFLICT, "IDEMPOTENCY_ERROR", exception.getMessage());
    }

    @ExceptionHandler(NotificationDispatchException.class)
    public ResponseEntity<Map<String, Object>> handleDispatch(NotificationDispatchException exception) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        logger.error("Unexpected error processing notification", exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Unexpected error processing notification");
    }

    @SuppressWarnings("null")
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String code, String error) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", code);
        payload.put("error", error);
        payload.put("timestamp", OffsetDateTime.now().toString());
        return ResponseEntity.status(status).body(payload);
    }
}
