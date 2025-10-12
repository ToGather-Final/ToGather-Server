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
                @Index(name = "idx_payments_session", columnList = "session_id"),
                @Index(name = "idx_payments_payer", columnList = "payer_account_id"),
                @Index(name = "idx_payments_merchant", columnList = "merchant_id")

        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "payer_account_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID payerAccountId;

    @Column(name = "merchant_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID merchantId;

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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    @Builder
    public Payment(UUID id, String sessionId, UUID payerAccountId, UUID merchantId, long amount, PaymentStatus status, String currency, String clientRequestId, LocalDateTime createdAt, LocalDateTime postedAt, String failureReason) {
        this.id = id != null ? id : UUID.randomUUID();
        this.sessionId = sessionId;
        this.payerAccountId = payerAccountId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.status = status;
        this.currency = currency;
        this.clientRequestId = clientRequestId;
        this.postedAt = postedAt;
        this.failureReason = failureReason;
    }

    public static Payment create(String sessionId, UUID payerAccountId, UUID merchantId, long amount, String clientRequestId) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.sessionId = sessionId;
        payment.payerAccountId = payerAccountId;
        payment.merchantId = merchantId;
        payment.amount = amount;
        payment.clientRequestId = clientRequestId;
        payment.status = PaymentStatus.PENDING;
        payment.currency = "KRW";
        return payment;
    }

    public void markAsSucceeded() {
        this.status = PaymentStatus.SUCCEEDED;
        this.postedAt = LocalDateTime.now();
    }

    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.postedAt = LocalDateTime.now();
    }

    public boolean isSucceeded() {
        return this.status == PaymentStatus.SUCCEEDED;
    }

    public boolean isFailed() {
        return this.status == PaymentStatus.FAILED;
    }

    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }
}
