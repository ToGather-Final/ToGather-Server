package com.example.trading_service.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "trade")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "trade_id", nullable = false, updatable = false)
    private UUID tradeId; // 체결 아이디 (PK)

    @Column(name = "order_id", nullable = false)
    private UUID orderId; // 주문 아이디 (FK, 실제로는 값만 저장)

    @Column(name = "stock_code", nullable = false)
    private String stockCode; // 주식 코드

    @Column(name = "stock_name", nullable = false)
    private String stockName; // 주식명

    @Column(name = "quantity", nullable = false)
    private float quantity; // 체결 수량

    @Column(name = "price", nullable = false)
    private float price; // 체결가

    @Column(name = "trade_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Order.OrderType tradeType; // 매수/매도 구분

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일자

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}