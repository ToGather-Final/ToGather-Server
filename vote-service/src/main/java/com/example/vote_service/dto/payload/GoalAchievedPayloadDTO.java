package com.example.vote_service.dto.payload;

/**
 * 목표 달성 페이로드 DTO
 */
public record GoalAchievedPayloadDTO(
        Integer targetAmount      // 목표 금액
) {
}
