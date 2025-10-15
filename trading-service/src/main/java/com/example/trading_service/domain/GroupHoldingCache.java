package com.example.trading_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "group_holding_cache")
public class GroupHoldingCache {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "group_holding_id", nullable = false, updatable = false)
    private UUID groupHoldingId;

    @Column(name = "group_id", nullable = false)
    private UUID groupId; // 그룹 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity; // 그룹 전체 보유 수량

    @Column(name = "avg_cost", nullable = false)
    private float avgCost; // 평균 매입 단가

    @Column(name = "profit")
    private Float profit; // 평가손익 (optional)

    @Column(name = "evaluated_price")
    private Float evaluatedPrice; // 평가금액 (optional)

    @Column(name = "member_count", nullable = false)
    private int memberCount; // 그룹 멤버 수

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 마지막 수정 시간

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}






