package com.example.vote_service.dto;

import com.example.vote_service.model.VoteChoice;
import jakarta.validation.constraints.NotNull;

/**
 * 투표 요청 DTO
 */
public record VoteRequest(
        @NotNull VoteChoice choice  // AGREE: 찬성, DISAGREE: 반대
) {
}

