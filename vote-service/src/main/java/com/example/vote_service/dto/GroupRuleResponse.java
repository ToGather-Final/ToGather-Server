package com.example.vote_service.dto;

/**
 * 그룹 규칙 응답 DTO
 * - user-service의 GET /groups/{groupId}/rules 응답
 */
public record GroupRuleResponse(
        int voteQuorum  // 투표 정족수
) {
}
