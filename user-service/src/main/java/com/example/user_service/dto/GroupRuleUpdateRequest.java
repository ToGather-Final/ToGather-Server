package com.example.user_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GroupRuleUpdateRequest(
        @NotNull(message = "투표 가결 인원수를 입력하사ㅔ요")
        @Positive(message = "투표 가결 인원수는 1명 이상이어야 합니다")
        Integer voteQuorum
) {
}
