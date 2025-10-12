package com.example.user_service.error;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record ApiError(String code, String message, String path, String timestamp) {
    public ApiError(String code, String message, String path, LocalDateTime timestamp) {
        this(code, message, path, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
