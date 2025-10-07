package com.example.vote_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 그룹 멤버 엔티티
 * - 사용자와 그룹의 다대다 관계를 나타내는 연결 테이블
 * - 복합 기본 키 사용 (userId + groupId)
 */
@Entity
@Table(name = "group_members")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@IdClass(GroupMembersId.class)
public class GroupMembers {

    @Id
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;

    @Id
    @Column(name = "group_id", columnDefinition = "BINARY(16)")
    private UUID groupId;

    @CreatedDate
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;
}
