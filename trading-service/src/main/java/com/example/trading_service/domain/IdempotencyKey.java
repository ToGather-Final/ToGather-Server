package com.example.trading_service.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_idem_key_payer", columnNames = {"idempotency_key", "payer_account_id"})
        },
        indexes = {
                @Index(name = "idx_idem_payer", columnList = "payer_account_id"),
                @Index(name = "idx_idem_payment", columnList = "payment_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKey {
    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "payer_account_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID payerAccountId;

    @Column(name = "payment_id", columnDefinition = "BINARY(16)")
    private UUID paymentId;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public IdempotencyKey(UUID id, String idempotencyKey, UUID payerAccountId,
                          UUID paymentId, Boolean isUsed, LocalDateTime createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.idempotencyKey = idempotencyKey;
        this.payerAccountId = payerAccountId;
        this.paymentId = paymentId;
        this.isUsed = isUsed;
        this.createdAt = createdAt;
    }

    public static IdempotencyKey create(String idempotencyKey, UUID payerAccountId) {
        IdempotencyKey key = new IdempotencyKey();
        key.id = UUID.randomUUID();
        key.idempotencyKey = idempotencyKey;
        key.payerAccountId = payerAccountId;
        key.isUsed = false;
        return key;
    }

    public void markAsUsed(UUID paymentId) {
        this.isUsed = true;
        this.paymentId = paymentId;
    }

    public boolean isUsed() {
        return this.isUsed;
    }
}
