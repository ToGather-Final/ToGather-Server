package com.example.vote_service.dto;

import com.example.vote_service.model.ProposalAction;
import com.example.vote_service.model.ProposalCategory;
import com.example.vote_service.model.ProposalStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 제안 응답 DTO
 */
public record ProposalResponse(
        UUID proposalId,
        UUID groupId,
        UUID userId,
        String proposalName,
        ProposalCategory category,
        ProposalAction action,
        String payload,
        ProposalStatus status,
        LocalDateTime openAt,
        LocalDateTime closeAt,
        Long approveCount,  // 찬성 투표 수
        Long rejectCount,   // 반대 투표 수
        Boolean hasVoted    // 현재 사용자가 투표했는지
) {
}

