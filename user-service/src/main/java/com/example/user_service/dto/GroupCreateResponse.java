package com.example.user_service.dto;

import java.util.UUID;

public record GroupCreateResponse(
        UUID groupId,
        String invitationCode
) {
}
