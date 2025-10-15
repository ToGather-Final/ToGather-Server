package com.example.vote_service.dto;

/**
 * 그룹 멤버 수 응답 DTO
 * - user-service의 GET /groups/{groupId}/members/count 응답
 */
public record GroupMemberCountResponse(
        int count  // 그룹 멤버 수
) {
}
