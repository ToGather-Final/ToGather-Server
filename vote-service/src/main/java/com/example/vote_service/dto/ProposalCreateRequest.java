package com.example.vote_service.dto;

import com.example.vote_service.model.ProposalAction;
import com.example.vote_service.model.ProposalCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 제안 생성 요청 DTO
 */
public record ProposalCreateRequest(
        @NotNull UUID groupId,
        @NotBlank String proposalName,
        @NotNull ProposalCategory category,
        @NotNull ProposalAction action,
        String payload // JSON 형태의 추가 데이터 (예수금, 마도, 수량, 가격 등)
) {
}

