package com.example.vote_service.dto;

import java.util.UUID;

public record VoteTradingRequest(
        UUID voteId,
        UUID groupId,
        String action,
        String payload
) {
}
