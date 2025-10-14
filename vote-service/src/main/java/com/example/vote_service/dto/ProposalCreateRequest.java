package com.example.vote_service.dto;

import com.example.vote_service.model.ProposalAction;
import com.example.vote_service.model.ProposalCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 제안 생성 요청 DTO
 * - groupId는 백엔드에서 사용자의 그룹을 자동으로 조회하여 설정
 */
public record ProposalCreateRequest(
        @NotBlank String proposalName,
        @NotNull ProposalCategory category,
        @NotNull ProposalAction action,
        Object payload // JSON 객체 또는 문자열 (제안 이유 등)
) {
}
