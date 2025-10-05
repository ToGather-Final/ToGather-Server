package com.example.vote_service.dto;

import java.util.UUID;

/**
 * 투표 결과 응답 DTO
 */
public record VoteResultResponse(
        UUID proposalId,
        Long totalVotes,
        Long approveCount,
        Long rejectCount,
        Boolean isPassed  // 투표 통과 여부
) {
}

