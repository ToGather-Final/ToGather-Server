package com.example.trading_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "trade")
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "trade_id", nullable = false, updatable = false)
    private UUID tradeId; // 체결 아이디 (PK)

    @Column(name = "order_id", nullable = false)
    private UUID orderId; // 주문 아이디 (FK, 실제로는 값만 저장)

    @Column(name = "quantity", nullable = false)
    private float quantity; // 체결 수량

    @Column(name = "price", nullable = false)
    private float price; // 체결가

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일자

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}