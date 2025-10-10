package com.example.vote_service.model;

/**
 * 제안 상태 Enum
 * - OPEN: 투표 진행 중
 * - APPROVED: 승인됨
 * - REJECTED: 거부됨
 */
public enum ProposalStatus {
    OPEN,       // 투표 진행 중
    APPROVED,   // 승인됨 (가결)
    REJECTED    // 거부됨 (부결)
}

