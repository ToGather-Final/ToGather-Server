package com.example.user_service.dto;

import java.util.UUID;

public record GroupSummaryResponse(UUID groupId, String groupName) {
}
