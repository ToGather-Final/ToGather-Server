package com.example.vote_service.dto;

import com.example.vote_service.model.VoteChoice;

/**
 * 투표 응답 DTO
 * - 제안 이름과 투표 선택만 반환
 */
public record VoteResponse(
        String proposalName,  // 투표한 제안의 이름
        VoteChoice choice     // 찬성(AGREE) / 반대(DISAGREE)
) {
}
