package com.example.vote_service.dto.payload;

import java.util.UUID;

/**
 * 투표 생성 페이로드 DTO
 */
public record VoteCreatedPayloadDTO(
        UUID proposalId,
        String proposalName,
        String proposerName
) {
}
