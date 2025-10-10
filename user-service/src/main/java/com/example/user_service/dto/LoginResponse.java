package com.example.user_service.dto;

import java.util.UUID;

public record LoginResponse(String accessToken, String refreshToken, UUID userId) {
}
