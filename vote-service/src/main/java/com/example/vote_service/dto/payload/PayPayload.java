package com.example.vote_service.dto.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * PAY 카테고리 제안의 payload DTO
 * - 페이 머니 충전 관련 정보를 담음
 */
public record PayPayload(
        @NotBlank String reason,                    // 제안 이유
        @NotNull @Positive Integer amountPerPerson  // 인당 충전 금액
) {
}
