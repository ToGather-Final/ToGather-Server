package com.example.user_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "group_members")
public class GroupMember {

    @EmbeddedId
    private GroupMemberId id;

    @Column(name = "userId", columnDefinition = "BINARY(16)", nullable = false, insertable = false, updatable = false)
    private UUID userId;

    @Column(name = "groupId", columnDefinition = "BINARY(16)", nullable = false, insertable = false, updatable = false)
    private UUID groupId;

    @Column(name = "joinedAt", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }

    public static GroupMember join(UUID groupId, UUID userId) {
        GroupMember member = new GroupMember();
        member.id = new GroupMemberId(userId, groupId);
        member.userId = userId;
        member.groupId = groupId;
        return member;
    }
}
