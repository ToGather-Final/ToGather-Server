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
@Table(name = "transfer",
        indexes = {
                @Index(name = "idx_transfers_from", columnList = "from_account_id"),
                @Index(name = "idx_transfers_to", columnList = "to_account_id"),
                @Index(name = "idx_transfers_client_request", columnList = "client_request_id")

        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transfer {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "from_account_id", columnDefinition = "BINARY(16)", nullable = true)
    private UUID fromAccountId;

    @Column(name = "to_account_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID toAccountId;

    @Column(nullable = false)
    @Check(constraints = "amount > 0")
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransferStatus status = TransferStatus.PENDING;

    @Column(length = 200)
    private String memo;

    @Column(name = "client_request_id", length = 64)
    private String clientRequestId;

    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    public Transfer(UUID id, UUID fromAccountId, UUID toAccountId, Long amount, String memo, String clientRequestId) {
        this.id = id != null ? id : UUID.randomUUID();
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.memo = memo;
        this.clientRequestId = clientRequestId;
        this.status = TransferStatus.PENDING;
    }

    public static Transfer create(UUID fromAccountId, UUID toAccountId, Long amount, String clientRequestId) {
        return Transfer.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(amount)
                .memo("페이머니 충전")
                .clientRequestId(clientRequestId)
                .build();
    }

    public void markAsSucceeded() {
        this.status = TransferStatus.SUCCESS;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsFailed(String reason) {
        this.status = TransferStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isSuccess() {
        return this.status == TransferStatus.SUCCESS;
    }
}
