package com.example.vote_service.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Proposal 엔티티
 * - 투표 제안을 나타냄
 * - category: 제안 유형 (TRADE, GROUP_NAME 등)
 * - action: 구체적인 행동 (BUY/SELL, DEPOSIT, CHARGE, ENABLE)
 * - status: 제안 상태 (OPEN, APPROVED, REJECTED)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "proposal")
public class Proposal {

    @Id
    @org.hibernate.annotations.UuidGenerator
    @Column(name = "proposalId", columnDefinition = "BINARY(16)")
    private UUID proposalId;

    @Column(name = "groupId", columnDefinition = "BINARY(16)", nullable = false)
    private UUID groupId;

    @Column(name = "userId", columnDefinition = "BINARY(16)", nullable = false)
    private UUID userId; // 제안자

    @Column(name = "proposalName", nullable = false, length = 255)
    private String proposalName; // 제안 이름

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private ProposalCategory category; // TRADE, PAY, GROUP_NAME 등

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private ProposalAction action; // BUY, SELL, DEPOSIT, CHARGE, ENABLE 등

    @Column(name = "payload", columnDefinition = "JSON")
    private String payload; // 제안 상세 데이터 (JSON 형태)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProposalStatus status; // OPEN, APPROVED, REJECTED

    @Column(name = "openAt", nullable = false)
    private LocalDateTime openAt; // 제안 시작 시간

    @Column(name = "closeAt")
    private LocalDateTime closeAt; // 제안 종료 시간

    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ProposalStatus.OPEN;
        }
        if (this.openAt == null) {
            this.openAt = LocalDateTime.now();
        }
    }

    /**
     * 정적 팩토리 메서드 - Proposal 생성
     */
    public static Proposal create(UUID groupId, UUID userId, String proposalName, 
                                   ProposalCategory category, ProposalAction action, 
                                   String payload, LocalDateTime closeAt) {
        Proposal proposal = new Proposal();
        proposal.groupId = groupId;
        proposal.userId = userId;
        proposal.proposalName = proposalName;
        proposal.category = category;
        proposal.action = action;
        proposal.payload = payload;
        proposal.closeAt = closeAt;
        proposal.status = ProposalStatus.OPEN;
        return proposal;
    }

    /**
     * 제안 승인 처리
     */
    public void approve() {
        if (this.status != ProposalStatus.OPEN) {
            throw new IllegalStateException("열린 제안만 승인할 수 있습니다.");
        }
        this.status = ProposalStatus.APPROVED;
        this.closeAt = LocalDateTime.now();
    }

    /**
     * 제안 거부 처리
     */
    public void reject() {
        if (this.status != ProposalStatus.OPEN) {
            throw new IllegalStateException("열린 제안만 거부할 수 있습니다.");
        }
        this.status = ProposalStatus.REJECTED;
        this.closeAt = LocalDateTime.now();
    }

    /**
     * 제안이 진행 중인지 확인
     */
    public boolean isOpen() {
        return this.status == ProposalStatus.OPEN;
    }
}

