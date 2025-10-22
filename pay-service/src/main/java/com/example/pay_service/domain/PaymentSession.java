package com.example.pay_service.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PaymentSession {
    private String id;
    private UUID groupId;
    private UUID payerUserId;
    private Long amount;
    private String recipientBankCode;
    private String recipientAccountNumber;
    private String recipientName;
    private String recipientBankName;
    private LocalDateTime expiresAt;
    private Boolean isUsed = false;
    private LocalDateTime createdAt;

    public static PaymentSession create(String id, UUID groupId, UUID payerUserId, Long amount,
                                        String recipientBankCode, String recipientAccountNumber,
                                        String recipientName, String recipientBankName,
                                        int ttlMinutes) {
        return PaymentSession.builder()
                .id(id)
                .groupId(groupId)
                .payerUserId(payerUserId)
                .amount(amount)
                .recipientBankCode(recipientBankCode)
                .recipientAccountNumber(recipientAccountNumber)
                .recipientName(recipientName)
                .recipientBankName(recipientBankName)
                .expiresAt(LocalDateTime.now().plusMinutes(ttlMinutes))
                .isUsed(false)
                .build();
    }

    public PaymentSession markAsUsed() {
        return PaymentSession.builder()
                .id(this.id)
                .groupId(this.groupId)
                .payerUserId(this.payerUserId)
                .amount(this.amount)
                .recipientBankCode(this.recipientBankCode)
                .recipientAccountNumber(this.recipientAccountNumber)
                .recipientName(this.recipientName)
                .recipientBankName(this.recipientBankName)
                .expiresAt(this.expiresAt)
                .isUsed(true)  // 사용됨으로 표시
                .createdAt(this.createdAt)
                .build();
    }

    public boolean isUsed() {
        return this.isUsed;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isValid() {
        return !isUsed && !isExpired();
    }

    public String getRecipientDisplayName() {
        return recipientName + " (" + recipientBankName + ")";
    }

    public String getRecipientAccountDisplay() {
        return recipientBankName + " " + getMaskedAccountNumber();
    }

    public String getMaskedAccountNumber() {
        if (recipientAccountNumber == null || recipientAccountNumber.length() < 4) {
            return recipientAccountNumber;
        }
        return "****" + recipientAccountNumber.substring(recipientAccountNumber.length() - 4);
    }

    // 세션 정보 요약
    public String getSessionSummary() {
        return String.format("PaymentSession[%s] %d원 -> %s",
                id, amount, getRecipientDisplayName());
    }
}
