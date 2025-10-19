package com.example.user_service.dto;

import java.util.UUID;

public record InviteAcceptResponse(
        UUID groupId,
        String groupName
) {
}
