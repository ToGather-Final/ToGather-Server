package com.example.trading_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "investment_account")
public class InvestmentAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "investment_account_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID investmentAccountId;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "account_no", nullable = false, unique = true)
    private String accountNo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}