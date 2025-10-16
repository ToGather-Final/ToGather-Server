package com.example.user_service.dto;

import jakarta.validation.constraints.Positive;

import java.util.Optional;

public record GroupSettingsUpdateRequest(
        @Positive(message = "투표 찬성 인원수는 1명 이상이어야 합니다")
        Optional<Integer> voteQuorum,

        @Positive(message = "그룹 해체 인원수는 1명 이상이어야 합니다")
        Optional<Integer> dissolutionQuorum,

        @Positive(message = "목표 금액은 0원보다 커야 합니다")
        Optional<Integer> goalAmount
        ) {
}
