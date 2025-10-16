package com.example.vote_service.event;

import java.util.UUID;

/**
 * 투표 마감 이벤트
 * - 투표 마감 시간에 발행되는 이벤트
 * - ProposalService에서 VoteService로 순환 참조 없이 전달
 */
public record VoteExpirationEvent(
        UUID proposalId,
        UUID groupId
) {
}
