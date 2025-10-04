package com.example.trading_service.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "order")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "investment_account_id", nullable = false)
    private UUID investmentAccountId; // FK, 실제로는 UUID 값만

    @Column(name = "stock_id", nullable = false)
    private UUID stockId; // FK, 실제로는 UUID 값만

    @Column(name = "order_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderType orderType; // 주문 종류

    @Column(name = "quantity", nullable = false)
    private float quantity; // 주문 수량

    @Column(name = "price", nullable = false)
    private float price; // 주문가(지정가/시장가)

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status; // 주문 상태

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일자

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정일자

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum OrderType {
        BUY, SELL
    }

    public enum Status {
        PENDING, FILLED, CANCELLED
    }
}