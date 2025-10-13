package com.example.trading_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "balance_cache")
public class BalanceCache {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "balance_id", nullable = false, updatable = false)
    private UUID balanceId; // 현금 아이디 (PK)

    @Column(name = "investment_account_id", nullable = false)
    private UUID investmentAccountId; // 주식 계좌 아이디

    @Column(name = "balance", nullable = false)
    private int balance; // 현금 잔고

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 마지막 갱신일

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}