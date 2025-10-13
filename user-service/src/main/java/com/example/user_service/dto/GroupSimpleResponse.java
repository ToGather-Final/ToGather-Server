package com.example.user_service.dto;

import java.util.UUID;

public record GroupSimpleResponse(
        UUID groupId,
        String groupName
) {
}
