package com.example.pay_service.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments",
        indexes = {
                @Index(name = "idx_payments_payer", columnList = "payer_account_id"),
                @Index(name = "idx_payments_recipient", columnList = "recipient_bank_code"),
                @Index(name = "idx_payments_created_at", columnList = "created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "payer_account_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID payerAccountId;

    @Column(nullable = false)
    @Check(constraints = "amount > 0")
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(length = 3)
    private String currency = "KRW";

    @Column(name = "client_request_id", length = 64)
    private String clientRequestId;

    // 수취 계좌 정보 (일회성 결제세션에서 가져온 정보)
    @Column(name = "recipient_bank_code", length = 10)
    private String recipientBankCode;

    @Column(name = "recipient_account_number", length = 20)
    private String recipientAccountNumber;

    @Column(name = "recipient_name", length = 50)
    private String recipientName;

    @Column(name = "recipient_bank_name", length = 50)
    private String recipientBankName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    @Builder
    public Payment(UUID id, UUID payerAccountId, long amount,
                   PaymentStatus status, String currency, String clientRequestId,
                   String recipientBankCode, String recipientAccountNumber,
                   String recipientName, String recipientBankName,
                   LocalDateTime createdAt, LocalDateTime postedAt, String failureReason) {
        this.id = id != null ? id : UUID.randomUUID();
        this.payerAccountId = payerAccountId;
        this.amount = amount;
        this.status = status;
        this.currency = currency;
        this.clientRequestId = clientRequestId;
        this.recipientBankCode = recipientBankCode;
        this.recipientAccountNumber = recipientAccountNumber;
        this.recipientName = recipientName;
        this.recipientBankName = recipientBankName;
        this.postedAt = postedAt;
        this.failureReason = failureReason;
    }

    // 기존 방식 (직접 생성)
    public static Payment create(UUID payerAccountId, long amount,
                                 String clientRequestId, String recipientBankCode,
                                 String recipientAccountNumber, String recipientName,
                                 String recipientBankName) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.payerAccountId = payerAccountId;
        payment.amount = amount;
        payment.clientRequestId = clientRequestId;
        payment.recipientBankCode = recipientBankCode;
        payment.recipientAccountNumber = recipientAccountNumber;
        payment.recipientName = recipientName;
        payment.recipientBankName = recipientBankName;
        payment.status = PaymentStatus.PENDING;
        payment.currency = "KRW";
        return payment;
    }

    // 결제 성공 처리
    public void markAsSucceeded() {
        this.status = PaymentStatus.SUCCEEDED;
        this.postedAt = LocalDateTime.now();
    }

    // 결제 실패 처리
    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.postedAt = LocalDateTime.now();
    }

    // 상태 확인 메서드들
    public boolean isSucceeded() {
        return this.status == PaymentStatus.SUCCEEDED;
    }

    public boolean isFailed() {
        return this.status == PaymentStatus.FAILED;
    }

    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }

    // 수취 계좌 정보 마스킹 (보안을 위해)
    public String getMaskedRecipientAccountNumber() {
        if (recipientAccountNumber == null || recipientAccountNumber.length() < 4) {
            return recipientAccountNumber;
        }
        return "****" + recipientAccountNumber.substring(recipientAccountNumber.length() - 4);
    }

    // 수취인 표시명 (화면에 표시용)
    public String getRecipientDisplayName() {
        return recipientName + " (" + recipientBankName + ")";
    }

    // 수취 계좌 정보 조합 (화면에 표시용)
    public String getRecipientAccountDisplay() {
        return recipientBankName + " " + getMaskedRecipientAccountNumber();
    }

    // 결제 정보 요약 (로그용)
    public String getPaymentSummary() {
        return String.format("Payment[%s] %d원 -> %s %s",
                id.toString().substring(0, 8),
                amount,
                getRecipientDisplayName(),
                getMaskedRecipientAccountNumber());
    }

    public static Payment createDirectPayment(UUID payerAccountId, Long amount,
                                              String recipientName, String recipientBankName,
                                              String recipientAccountNumber, String clientRequestId) {
        return Payment.builder()
                .payerAccountId(payerAccountId)
                .amount(amount)
                .currency("KRW")
                .status(PaymentStatus.PENDING)
                .recipientName(recipientName)
                .recipientBankName(recipientBankName)
                .recipientAccountNumber(recipientAccountNumber)
                .clientRequestId(clientRequestId)
                .build();
    }
}