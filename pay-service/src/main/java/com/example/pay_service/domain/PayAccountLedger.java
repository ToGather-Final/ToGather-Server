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
@Table(name = "pay_account_ledger",
        indexes = {
                @Index(name = "idx_ledger_pay_account", columnList = "pay_account_id"),
                @Index(name = "idx_ledger_transaction_type", columnList = "transaction_type"),
                @Index(name = "idx_ledger_created_at", columnList = "created_at")

        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayAccountLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pay_account_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID payAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 16)
    private TransactionType transactionType;

    @Column(nullable = false)
    @Check(constraints = "amount > 0")
    private long amount;

    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;

    @Column(length = 200)
    private String description;

    @Column(name = "related_payment_id", columnDefinition = "BINARY(16)")
    private UUID relatedPaymentId;

    @Column(name = "related_transfer_id", columnDefinition = "BINARY(16)")
    private UUID relatedTransferId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public PayAccountLedger(Long id, UUID payAccountId, TransactionType transactionType, long amount, long balanceAfter, String description, UUID relatedPaymentId, UUID relatedTransferId, LocalDateTime createdAt) {
        this.id = id;
        this.payAccountId = payAccountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.relatedPaymentId = relatedPaymentId;
        this.relatedTransferId = relatedTransferId;
        this.createdAt = createdAt;
    }

    public static PayAccountLedger create(UUID payAccountId, TransactionType transactionType, long amount, long balanceAfter, String description) {
        return PayAccountLedger.builder()
                .payAccountId(payAccountId)
                .transactionType(transactionType)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(description)
                .build();
    }
}

