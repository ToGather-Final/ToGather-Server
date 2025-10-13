package com.example.pay_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferRequest(
        @NotNull(message = "충전 금액을 입력하세요")
        @Positive(message = "충전 금액은 0원보다 커야 합니다")
        Long amount,

        String clientRequestId
) {
}
