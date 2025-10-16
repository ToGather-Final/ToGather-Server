package com.example.user_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QuorumUpdateRequest(
        @NotNull(message = "투표 찬성 인원수를 입력하세요")
        @Positive(message = "투표 찬성 인원수는 1명 이상이어야 합니다")
        Integer voteQuorum,

        @NotNull(message = "그룹 해체 인원수를 입력하세요")
        @Positive(message = "그룹 해체 인원수는 1명 이상이어야 합니다")
        Integer dissolutionQuorum
        ) {
}
