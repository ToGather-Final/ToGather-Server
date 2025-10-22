package com.example.vote_service.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
    @Column(name = "proposal_id", columnDefinition = "BINARY(16)")
    private UUID proposalId;

    @Column(name = "group_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID groupId;

    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID userId; // 제안자

    @Column(name = "proposal_name", nullable = false, length = 255)
    private String proposalName; // 제안 이름

    @Column(name = "proposer_name", nullable = false, length = 100)
    private String proposerName; // 제안자 닉네임

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private ProposalCategory category; // TRADE, PAY

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private ProposalAction action; // BUY, SELL, DEPOSIT, CHARGE

    @Column(name = "payload", columnDefinition = "JSON")
    private String payload; // 제안 상세 데이터 (reason 등. JSON 형태)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProposalStatus status; // OPEN, APPROVED, REJECTED

    @Column(name = "open_at", nullable = false)
    private LocalDateTime openAt; // 제안 시작 시간

    @Column(name = "close_at")
    private LocalDateTime closeAt; // 제안 종료 시간

    @Column(name = "created_at", nullable = false, updatable = false)
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
    public static Proposal create(UUID groupId, UUID userId, String proposalName, String proposerName,
                                   ProposalCategory category, ProposalAction action, 
                                   String payload, LocalDateTime closeAt) {
        Proposal proposal = new Proposal();
        proposal.groupId = groupId;
        proposal.userId = userId;
        proposal.proposalName = proposalName;
        proposal.proposerName = proposerName;
        proposal.category = category;
        proposal.action = action;
        proposal.payload = payload;
        proposal.closeAt = closeAt;
        proposal.status = ProposalStatus.OPEN;
        return proposal;
    }

    /**
     * 제안 승인 처리 (투표 집계 후 호출)
     */
    public void approve() {
        if (this.status != ProposalStatus.OPEN) {
            throw new IllegalStateException("열린 제안만 승인할 수 있습니다.");
        }
        this.status = ProposalStatus.APPROVED;
        if (this.closeAt == null) {
            this.closeAt = LocalDateTime.now();
        }
    }

    /**
     * 제안 거부 처리 (투표 집계 후 호출)
     */
    public void reject() {
        if (this.status != ProposalStatus.OPEN) {
            throw new IllegalStateException("열린 제안만 거부할 수 있습니다.");
        }
        this.status = ProposalStatus.REJECTED;
        if (this.closeAt == null) {
            this.closeAt = LocalDateTime.now();
        }
    }

    /**
     * 제안이 진행 중인지 확인
     */
    public boolean isOpen() {
        return this.status == ProposalStatus.OPEN;
    }

    /**
     * 투표 마감 시간이 지났는지 확인
     */
    public boolean isExpired() {
        if (this.closeAt == null) {
            return false;
        }

        ZoneId koreaZone = ZoneId.of("Asia/Seoul");
        LocalDateTime nowInKorea = LocalDateTime.now(koreaZone);

        return nowInKorea.isAfter(this.closeAt);
    }

    /**
     * 투표가 진행 가능한 상태인지 확인 (진행중 + 마감 전)
     */
    public boolean canVote() {
        return isOpen() && !isExpired();
    }

    /**
     * 테스트용 메서드 - proposalId 설정
     * 실제 운영에서는 JPA가 자동으로 생성하므로 사용하지 않음
     */
    public void setProposalIdForTest(UUID proposalId) {
        this.proposalId = proposalId;
    }
}

