package com.example.user_service.error;

import java.time.LocalDateTime;

public record ApiError(String code, String message, String path, LocalDateTime timestamp) {
}
