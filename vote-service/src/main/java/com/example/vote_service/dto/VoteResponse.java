package com.example.vote_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 투표 응답 DTO
 */
public record VoteResponse(
        UUID voteId,
        UUID proposalId,
        UUID userId,
        Boolean choice,
        LocalDateTime createdAt
) {
}

