package com.example.pay_service.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_sessions",
        indexes = {
                @Index(name = "idx_payment_sessions_group", columnList = "group_id"),
                @Index(name = "idx_payment_sessions_payer", columnList = "payer_user_id"),
                @Index(name = "idx_payment_sessions_expires", columnList = "expires_at"),
                @Index(name = "idx_payment_sessions_used", columnList = "is_used")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PaymentSession {
    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "group_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID groupId;

    @Column(name = "payer_user_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID payerUserId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "recipient_bank_code", length = 10)
    private String recipientBankCode;

    @Column(name = "recipient_account_number", length = 20)
    private String recipientAccountNumber;

    @Column(name = "recipient_name", length = 50)
    private String recipientName;

    @Column(name = "recipient_bank_name", length = 50)
    private String recipientBankName;

    @Column(name = "expires_at", nullable = false)
    @Check(constraints = "expires_at > created_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
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
