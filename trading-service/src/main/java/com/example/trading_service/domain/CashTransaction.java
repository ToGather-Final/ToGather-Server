package com.example.trading_service.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "cash_transaction")
public class CashTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cash_transaction_id", nullable = false, updatable = false)
    private UUID cashTransactionId; // 체결 아이디 (PK)

    @Column(name = "investment_account_id", nullable = false)
    private UUID investmentAccountId; // 주식 계좌 아이디 (FK, 값만 저장)

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type; // 종류 (DEPOSIT, WITHDRAW)

    @Column(name = "price", nullable = false)
    private int price; // 금액

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일자

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Type {
        DEPOSIT, WITHDRAW
    }
}