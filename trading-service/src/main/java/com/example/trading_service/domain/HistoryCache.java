package com.example.trading_service.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "history_cache")
public class HistoryCache {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "history_id", nullable = false, updatable = false)
    private UUID historyId; // 히스토리 아이디 (PK)

    @Column(name = "investment_account_id", nullable = false)
    private UUID investmentAccountId; // 주식 계좌 아이디

    @Column(name = "stock_id")
    private UUID stockId; // 주식 아이디 (optional)

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type; // 거래유형

    @Column(name = "ref_id", nullable = false)
    private UUID refId; // 실제 발생한 거래의 원본 PK

    @Column(name = "amount", nullable = false)
    private float amount; // 수량/금액

    @Column(name = "detail")
    private String detail; // 상세 설명

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일자

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Type {
        ORDER, TRADE, DEPOSIT, WITHDRAW, PROPOSAL, VOTE
    }
}