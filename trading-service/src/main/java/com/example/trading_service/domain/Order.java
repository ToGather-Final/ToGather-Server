package com.example.trading_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "orders")
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
        PENDING, FILLED, CANCELLED, COMPLETED
    }

    // 주문이 매수인지 확인
    public boolean isBuy() {
        return orderType == OrderType.BUY;
    }

    // 주문이 매도인지 확인
    public boolean isSell() {
        return orderType == OrderType.SELL;
    }

    // 주식 코드 반환 (임시로 하드코딩, 실제로는 Stock 엔티티와 조인 필요)
    public String getStockCode() {
        // TODO: 실제로는 Stock 엔티티와 조인하여 stockCode 반환
        return "005930"; // 임시로 삼성전자 코드 반환
    }

    // 주식명 반환 (임시로 하드코딩, 실제로는 Stock 엔티티와 조인 필요)
    public String getStockName() {
        // TODO: 실제로는 Stock 엔티티와 조인하여 stockName 반환
        return "삼성전자"; // 임시로 삼성전자명 반환
    }
}