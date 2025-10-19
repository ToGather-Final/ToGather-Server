package com.example.user_service.dto;

import com.example.user_service.domain.GroupStatus;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        String username,
        String nickname,
        List<UserGroupInfo> groups
) {
    public record UserGroupInfo(
            UUID groupId,
            String groupName,
            String groupCode,
            GroupStatus status,
            Integer currentMembers,
            Integer maxMembers,
            Boolean isFull,
            Boolean isOwner
    ) {
    }
}

