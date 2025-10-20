package com.example.trading_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "history")
public class History {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "history_id", nullable = false, updatable = false)
    private UUID historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investment_account_id", nullable = true)
    private InvestmentAccount investmentAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id")
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType; // BUY, SELL

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal price;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal totalAmount;

    @Column(name = "order_id")
    private UUID orderId; // 관련 주문 ID

    @Column(name = "group_id")
    private UUID groupId; // 그룹 거래인 경우 그룹 ID

    @Column(name = "date", nullable = false, length = 20)
    private String date;

    @Column(name = "history_category", length = 20)
    private String historyCategory;

    @Column(name = "history_type", length = 50)
    private String historyType;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "payload", columnDefinition = "JSON")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 거래 발생일

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.date = LocalDateTime.now().toString().substring(0, 19);
    }

    public enum TransactionType {
        BUY,  // 매수
        SELL  // 매도
    }
}






