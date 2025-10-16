package com.example.pay_service.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pay_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_group_pay_account_one",
                        columnNames = {"group_id"})
        },
        indexes = {
                @Index(name = "idx_pay_accounts_owner", columnList = "owner_user_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayAccount {
    @Id
    private UUID id;

    @Column(name = "owner_user_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private long balance;

    @Version
    private long version;

    @Column(length = 64)
    private String nickname;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(name = "group_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID groupId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public PayAccount(UUID id, UUID ownerUserId, long balance, String nickname, Boolean isActive, UUID groupId, long version) {
        this.id = id != null ? id : UUID.randomUUID();
        this.ownerUserId = ownerUserId;
        this.balance = balance;
        this.nickname = nickname;
        this.isActive = isActive;
        this.groupId = groupId;
        this.version = version;
    }

    public boolean hasSufficientBalance(long amount) {
        return this.balance >= amount;
    }

    public void debit(long amount) {
        if (!hasSufficientBalance(amount)) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        this.balance -= amount;
    }

    public void credit(long amount) {
        this.balance += amount;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.ownerUserId != null && this.ownerUserId.equals(userId);
    }
}
