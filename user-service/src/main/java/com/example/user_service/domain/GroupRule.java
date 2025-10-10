package com.example.user_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "group_rules")
public class GroupRule {

    @Id
    @Column(name = "groupId", columnDefinition = "BINARY(16)")
    private UUID groupId;

    @Column(name = "voteQuorum", nullable = false)
    private Integer voteQuorum;

    @Column(name = "voteDurationHours", nullable = false)
    private Integer voteDurationHours;

    @Column(name = "updatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void onWrite() {
        this.updatedAt = LocalDateTime.now();
    }

    public static GroupRule of(UUID groupId, Integer voteQuorum, Integer voteDurationHours) {
        GroupRule rule = new GroupRule();
        rule.groupId = groupId;
        rule.voteQuorum = voteQuorum;
        rule.voteDurationHours = voteDurationHours;
        return rule;
    }
}
