package com.example.trading_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "balance")
public class Balance {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "balance_id", nullable = false, updatable = false)
    private UUID balanceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investment_account_id", nullable = false)
    private InvestmentAccount investmentAccount;

    @Column(name = "balance", nullable = false)
    private int balance; // 현금 잔고

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 최초 생성일

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 마지막 갱신일

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}






