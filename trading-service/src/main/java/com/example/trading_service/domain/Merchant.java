package com.example.trading_service.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchants",
        indexes = {
                @Index(name = "idx_merchants_active", columnList = "is_active")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Merchant {
    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "settlement_account_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID settlementAccountId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "description", length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Merchant(UUID id, String displayName, UUID settlementAccountId, boolean isActive, String logoUrl, String description) {
        this.id = id != null ? id : UUID.randomUUID();
        this.displayName = displayName;
        this.settlementAccountId = settlementAccountId;
        this.isActive = isActive;
        this.logoUrl = logoUrl;
        this.description = description;
    }

    public void updateInfo(String displayName, String logoUrl, String description) {
        this.displayName = displayName;
        this.logoUrl = logoUrl;
        this.description = description;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }
}
