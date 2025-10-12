package com.example.user_service.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
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

    public static Group create(String groupName, UUID ownerId, Integer goalAmount, Integer initialAmount, Integer maxMembers, Integer dissolutionQuorum) {
        Group group = new Group();
        group.groupName = groupName;
        group.ownerId = ownerId;
        group.goalAmount = goalAmount;
        group.initialAmount = initialAmount;
        group.maxMembers = maxMembers;
        group.dissolutionQuorum = dissolutionQuorum;
        group.status = GroupStatus.WATING;
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
}
