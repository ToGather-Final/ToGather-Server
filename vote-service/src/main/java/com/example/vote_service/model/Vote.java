package com.example.vote_service.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vote 엔티티
 * - 개별 투표를 나타냄
 * - 각 사용자가 특정 Proposal에 대해 찬성/반대 투표
 * - choice: AGREE (찬성), DISAGREE (반대)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "vote")
public class Vote {

    @Id
    @org.hibernate.annotations.UuidGenerator
    @Column(name = "voteId", columnDefinition = "BINARY(16)")
    private UUID voteId;

    @Column(name = "proposalId", columnDefinition = "BINARY(16)", nullable = false)
    private UUID proposalId;

    @Column(name = "userId", columnDefinition = "BINARY(16)", nullable = false)
    private UUID userId; // 투표자

    @Enumerated(EnumType.STRING)
    @Column(name = "choice", nullable = false, length = 20)
    private VoteChoice choice; // AGREE: 찬성, DISAGREE: 반대

    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 정적 팩토리 메서드 - Vote 생성
     */
    public static Vote create(UUID proposalId, UUID userId, VoteChoice choice) {
        Vote vote = new Vote();
        vote.proposalId = proposalId;
        vote.userId = userId;
        vote.choice = choice;
        return vote;
    }

    /**
     * 투표 변경
     */
    public void changeChoice(VoteChoice newChoice) {
        if (newChoice == VoteChoice.NEUTRAL) {
            throw new IllegalArgumentException("투표는 AGREE 또는 DISAGREE만 가능합니다.");
        }
        this.choice = newChoice;
    }

    /**
     * 찬성 투표인지 확인
     */
    public boolean isApproval() {
        return this.choice == VoteChoice.AGREE;
    }

    /**
     * 반대 투표인지 확인
     */
    public boolean isRejection() {
        return this.choice == VoteChoice.DISAGREE;
    }

    /**
     * 테스트용 메서드 - voteId 설정
     * 실제 운영에서는 JPA가 자동으로 생성하므로 사용하지 않음
     */
    public void setVoteIdForTest(UUID voteId) {
        this.voteId = voteId;
    }
}

