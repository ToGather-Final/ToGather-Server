package com.example.trading_service.dto;

import com.example.trading_service.domain.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
        String paymentId,
        PaymentStatus status,
        Long amount,
        String currency,
        MerchantInfo merchant,
        String payerAccountId,
        LocalDateTime postedAt,
        Long balanceAfter,
        String failureReason
) {
    public record MerchantInfo(
            String id,
            String displayName
    ){}

    public static PaymentResponse createSuccessResponse(String paymentId, PaymentStatus status, Long amount, String currency, String merchantId, String merchantDisplayName, String payerAccountId, LocalDateTime postedAt, Long balanceAfter) {
        MerchantInfo merchantInfo = new MerchantInfo(merchantId, merchantDisplayName);

        return new PaymentResponse(
                paymentId, status, amount, currency, merchantInfo, payerAccountId, postedAt, balanceAfter, null
        );
    }

    public static PaymentResponse createFailedResponse(String paymentId, String failureReason) {
        return new PaymentResponse(
                paymentId, PaymentStatus.FAILED, null, null, null, null, null, null, failureReason
        );
    }
}
