package com.example.user_service.dto;

import java.util.UUID;

public record MeResponse(UUID userId, String username, String nickname) {
}
