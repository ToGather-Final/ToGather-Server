package com.example.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record GroupCreateRequest(
        @NotBlank(message = "그룹명을 입력하세요")
        String groupName,

        @NotNull(message = "목표 투자금을 입력하세요")
        @Positive(message = "목표 투자금은 0원보다 커야 합니다")
        Integer goalAmount,

        @PositiveOrZero(message = "초기 투자금은 0원 이상이어야 합니다")
        Integer initialAmount,

        @NotNull(message = "그룹 최대 인원을 입력하세요")
        @Positive(message = "그룹 인원은 1명 이상이어야 합니다")
        Integer maxMembers,

        @NotNull(message = "투표 가결 인원수를 입력하세요")
        @Positive(message = "투표 가결 인원수는 1명 이상이어야 합니다")
        Integer voteQuorum,

        @NotNull(message = "그룹 해체 동의 인원수를 입력하세요")
        @Positive(message = "그룹 해체 동의 인원수는 1명 이상이어야 합니다")
        Integer dissolutionQuorum
) {
}
