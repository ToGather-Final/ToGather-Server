package com.example.vote_service.dto;

import com.example.vote_service.model.ProposalCategory;
import com.example.vote_service.model.ProposalStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 제안 요약 응답 DTO (목록 조회용)
 */
public record ProposalSummaryResponse(
        UUID proposalId,
        String proposalName,
        String proposerName,
        ProposalCategory category,
        ProposalAction action,
        String payload,
        ProposalStatus status,
        String date,  // yyyy-MM-dd 형태 문자열
        Integer agreeCount,
        Integer disagreeCount,
        VoteChoice myVote  // AGREE, DISAGREE, NEUTRAL
) {
}

