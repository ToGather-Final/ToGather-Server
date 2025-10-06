package com.example.vote_service.error;

import java.time.LocalDateTime;

/**
 * API 에러 응답 DTO
 */
public record ApiError(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, LocalDateTime.now());
    }
}

