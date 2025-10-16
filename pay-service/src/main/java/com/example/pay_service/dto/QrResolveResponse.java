package com.example.pay_service.dto;

import java.time.LocalDateTime;
import java.util.List;

public record QrResolveResponse(
        String paymentSessionId,
        RecipientInfo recipient,
        Long suggestedAmount,
        List<PayerAccount> payerAccounts,
        LocalDateTime expiresAt
) {
    public record RecipientInfo(
            String recipientName,
            String bankName,
            String maskedAccountNo,
            String logoUrl
    ) {}

    public record PayerAccount(
            String accountId,
            String accountType,
            String displayName,
            Long balance
    ) {}
}
