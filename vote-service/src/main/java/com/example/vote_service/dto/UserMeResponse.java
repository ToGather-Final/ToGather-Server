package com.example.vote_service.dto;

/**
 * 현재 인증된 사용자 정보 응답 DTO
 * - user-service의 GET /users/me 응답
 */
public record UserMeResponse(
        String userId,
        String username,
        String nickname
) {
}
