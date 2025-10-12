package com.example.user_service.error;

public record ApiError(String code, String message, String path, String timestamp) {
}
