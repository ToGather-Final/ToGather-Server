package com.example.trading_service.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "holding_cache")
public class HoldingCache {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "holding_id", nullable = false, updatable = false)
    private UUID holdingId; // PK

    @Column(name = "investment_account_id", nullable = false)
    private UUID investmentAccountId; // 주식 계좌 아이디

    @Column(name = "stock_id", nullable = false)
    private UUID stockId; // 주식 아이디

    @Column(name = "quantity", nullable = false)
    private int quantity; // 현재 보유 수량

    @Column(name = "avg_cost", nullable = false)
    private float avgCost; // 평균 매입 단가

    @Column(name = "profit")
    private Float profit; // 평가손익 (optional)

    @Column(name = "evaluated_price")
    private Float evaluatedPrice; // 평가금액 (optional)

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 마지막 수정 시간

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}