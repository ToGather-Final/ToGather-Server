package com.example.user_service.dto;

import com.example.user_service.domain.GroupStatus;

public record GroupStatusResponse(
        GroupStatus status,
        Integer currentMembers,
        Integer maxMembers,
        Boolean isFull
) {
}
