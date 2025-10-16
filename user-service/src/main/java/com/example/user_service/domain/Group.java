package com.example.user_service.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "`groups`")
public class Group {

    @Id
    @org.hibernate.annotations.UuidGenerator
    @Column(name = "groupId", columnDefinition = "BINARY(16)")
    private UUID groupId;

    @Column(name = "groupName", nullable = false, length = 100)
    private String groupName;

    @Column(name = "ownerId", columnDefinition = "BINARY(16)", nullable = false)
    private UUID ownerId;

    @Column(name = "goalAmount", nullable = false)
    private Integer goalAmount;

    @Column(name = "initialAmount", nullable = false)
    private Integer initialAmount;

    @Column(name = "maxMembers", nullable = false)
    private Integer maxMembers;

    @Column(name = "voteQuorum", nullable = false)
    private Integer voteQuorum;

    @Column(name = "dissolutionQuorum",nullable = false)
    private Integer dissolutionQuorum;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GroupStatus status;

    @Column(name = "currentMembers", nullable = false)
    private Integer currentMembers;

    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    public static Group create(String groupName, UUID ownerId, Integer goalAmount, Integer initialAmount, Integer maxMembers,Integer voteQuorum, Integer dissolutionQuorum) {
        Group group = new Group();
        group.groupName = groupName;
        group.ownerId = ownerId;
        group.goalAmount = goalAmount;
        group.initialAmount = initialAmount;
        group.maxMembers = maxMembers;
        group.voteQuorum = voteQuorum;
        group.dissolutionQuorum = dissolutionQuorum;
        group.status = GroupStatus.WAITING;
        group.currentMembers = 1;
        return group;
    }

    public void addMember() {
        this.currentMembers++;
        if (this.currentMembers >= this.maxMembers) {
            this.status = GroupStatus.ACTIVE;
        }
    }

    public boolean isFull() {
        return this.currentMembers >= this.maxMembers;
    }

    public boolean isWaiting() {
        return this.status == GroupStatus.WAITING;
    }

    public void updateSettings(Optional<Integer> voteQuorum,
                               Optional<Integer> dissolutionQuorum,
                               Optional<Integer> goalAmount) {

        voteQuorum.ifPresent(quorum -> {
            if (quorum <= 0) {
                throw new IllegalArgumentException("투표 찬성 인원수는 1명 이상이어야 합니다");
            }
            this.voteQuorum = quorum;
        });

        dissolutionQuorum.ifPresent(quorum -> {
            if (quorum <= 0) {
                throw new IllegalArgumentException("그룹 해체 인원수는 1명 이상이어야 합니다");
            }
            this.dissolutionQuorum = quorum;
        });

        goalAmount.ifPresent(amount -> {
            if (amount <= 0) {
                throw new IllegalArgumentException("목표 금액은 0원보다 커야 합니다");
            }
            this.goalAmount = amount;
        });
    }

    public void updateGoalAmount(Integer goalAmount) {
        if (goalAmount == null || goalAmount <= 0) {
            throw new IllegalArgumentException("목표 금액은 0원보다 커야 합니다");
        }
        this.goalAmount = goalAmount;
    }

    public void updateQuorumSetting(Integer voteQuorum, Integer dissolutionQuorum) {
        if (voteQuorum == null || voteQuorum <= 0) {
            throw new IllegalArgumentException("투표 찬성 인원수는 1명 이상이어야 합니다");
        }
        if (dissolutionQuorum == null || dissolutionQuorum <= 0) {
            throw new IllegalArgumentException("그룹 해체 인원수는 1명 이상이어야 합니다");
        }

        this.voteQuorum = voteQuorum;
        this.dissolutionQuorum = dissolutionQuorum;
    }
}
