package com.example.user_service.dto;

import java.util.UUID;

public record GroupMemberSimple(
        UUID userId,
        String nickname,
        String role) {
}
