package com.example.vote_service.dto.payload;

import java.util.UUID;

/**
 * 투표 부결 페이로드 DTO
 */
public record VoteRejectedPayloadDTO(
        UUID proposalId,
        String proposalName
) {
}
