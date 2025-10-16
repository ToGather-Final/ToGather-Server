package com.example.user_service.dto;

public record GroupSettingsResponse(
        Integer voteQuorum,
        Integer dissolutionQuorum,
        Integer goalAmount,
        Integer maxMembers
) {
}
