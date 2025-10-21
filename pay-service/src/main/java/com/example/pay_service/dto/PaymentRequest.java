package com.example.pay_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRequest(
        @NotBlank(message = "출금 계좌 ID는 필수입니다")
        String payerAccountId,

        @NotNull(message = "결제 금액은 필수입니다")
        @Positive(message = "결제 금액은 0보다 커야 합니다")
        Long amount,

        @NotBlank(message = "수취인명은 필수입니다")
        String recipientName,

        @NotBlank(message = "수취인 은행명은 필수입니다")
        String recipientBankName,

        @NotBlank(message = "수취인 계좌번호는 필수입니다")
        String recipientAccountNumber,

        String clientRequestId
) {}
