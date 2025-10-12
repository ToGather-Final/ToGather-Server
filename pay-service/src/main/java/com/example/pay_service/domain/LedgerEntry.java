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
@Table(name = "ledger_entries",
        indexes = {
                @Index(name = "idx_ledger_tx", columnList = "tx_id"),
                @Index(name = "idx_ledger_debit", columnList = "debit_account_id"),
                @Index(name = "idx_ledger_credit", columnList = "credit_account_id")

        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tx_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID txId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_side", nullable = false, length = 8)
    private EntrySide entrySide;

    @Column(name = "debit_account_id", columnDefinition = "BINARY(16)")
    private UUID debitAccountId;

    @Column(name = "credit_account_id", columnDefinition = "BINARY(16)")
    private UUID creditAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(nullable = false)
    @Check(constraints = "amount > 0")
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TxType type;

    @Column(name = "related_payment_id", columnDefinition = "BINARY(16)")
    private UUID relatedPaymentId;

    @Column(length = 120)
    private String memo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public LedgerEntry(Long id, UUID txId, EntrySide entrySide, UUID debitAccountId, UUID creditAccountId, long amount, TxType type, UUID relatedPaymentId, String memo, LocalDateTime createdAt) {
        this.id = id;
        this.txId = txId;
        this.entrySide = entrySide;
        this.debitAccountId = debitAccountId;
        this.creditAccountId = creditAccountId;
        this.amount = amount;
        this.type = type;
        this.relatedPaymentId = relatedPaymentId;
        this.memo = memo;
        this.createdAt = createdAt;
    }

    public static LedgerEntry createDebitEntry(UUID txId, UUID debitAccountId, long amount, TxType type, UUID paymentId, String memo) {
        LedgerEntry entry = new LedgerEntry();
        entry.txId = txId;
        entry.entrySide = EntrySide.DEBIT;
        entry.debitAccountId = debitAccountId;
        entry.amount = amount;
        entry.type = type;
        entry.relatedPaymentId = paymentId;
        entry.memo = memo;
        return entry;
    }

    public static LedgerEntry createCreditEntry(UUID txId, UUID creditAccountId, long amount, TxType type, UUID paymentId, String memo) {
        LedgerEntry entry = new LedgerEntry();
        entry.txId = txId;
        entry.entrySide = EntrySide.CREDIT;
        entry.creditAccountId = creditAccountId;
        entry.amount = amount;
        entry.type = type;
        entry.relatedPaymentId = paymentId;
        entry.memo = memo;
        return entry;
    }

    public void markAsCompleted() {
        this.status = TransactionStatus.COMPLETED;
    }

    public void markAsFailed() {
        this.status = TransactionStatus.FAILED;
    }

    public boolean isCompleted() {
        return this.status == TransactionStatus.COMPLETED;
    }


}
