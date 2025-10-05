package com.example.vote_service.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 투표 요청 DTO
 */
public record VoteRequest(
        @NotNull Boolean choice  // true: 찬성, false: 반대
) {
}

