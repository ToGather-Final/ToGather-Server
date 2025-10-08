package com.example.trading_service.dto;

import com.example.trading_service.domain.AccountType;

import java.time.LocalDateTime;
import java.util.List;

public record QrResolveResponse(
        String paymentSessionId,
        MerchantInfo merchant,
        Long suggestedAmount,
        List<PayerAccount> payerAccounts,
        LocalDateTime expiresAt
) {
    public record MerchantInfo(
            String id,
            String displayName,
            String bank,
            String accountNoMasked,
            String logoUrl
    ) {}

    public record PayerAccount(
            String id,
            AccountType type,
            String displayName,
            Long balance
    ) {}
}
