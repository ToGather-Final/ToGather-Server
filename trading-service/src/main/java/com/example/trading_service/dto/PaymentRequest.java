package com.example.trading_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRequest(
        @NotBlank(message = "결제 세션 ID는 필수입니다")
        String paymentSessionId,

        @NotBlank(message = "출금 계좌 ID는 필수입니다")
        String payerAccountId,

        @NotNull(message = "결제 금액은 필수입니다")
        @Positive(message = "결제 금액은 0보다 커야 합니다")
        Long amount,

        String clientRequestId
) {}
