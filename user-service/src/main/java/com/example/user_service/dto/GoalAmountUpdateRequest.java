package com.example.user_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GoalAmountUpdateRequest(
        @NotNull(message = "목표 금액을 입력하세요")
        @Positive(message = "목표 금액은 0원보다 커야 합니다")
        Integer goalAmount
) {
}
