package com.example.vote_service.dto;

import java.util.List;

/**
 * 히스토리 목록 응답 DTO
 */
public record GetHistoryResponse(
        List<HistoryDTO> items,
        String nextCursor
) {
}
