package com.example.vote_service.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * GroupMembers 복합 기본 키 클래스
 * - userId와 groupId의 조합으로 고유성 보장
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GroupMembersId implements Serializable {
    private UUID userId;
    private UUID groupId;
}
