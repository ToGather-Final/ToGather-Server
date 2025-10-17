package com.example.vote_service.dto;

import com.example.vote_service.model.HistoryCategory;
import com.example.vote_service.model.HistoryType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

/**
 * 히스토리 응답 DTO
 * - 프론트엔드 HistoryDTO와 매핑
 */
public record HistoryDTO(
        UUID id,                    // historyId
        HistoryCategory category,   // VOTE, TRADE, CASH, PAY, GOAL
        HistoryType type,           // VOTE_CREATED, VOTE_APPROVED, etc.
        String title,               // 카드 타이틀
        String date,                // 날짜 표기
        Object payload              // 타입별 페이로드
) {
}
