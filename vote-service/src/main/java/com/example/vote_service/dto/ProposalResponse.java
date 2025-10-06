package com.example.vote_service.dto;

import com.example.vote_service.model.ProposalAction;
import com.example.vote_service.model.ProposalCategory;
import com.example.vote_service.model.ProposalStatus;
import com.example.vote_service.model.VoteChoice;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 제안 응답 DTO
 */
public record ProposalResponse(
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

