package com.example.vote_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 거래 실패 히스토리 저장 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TradeFailedHistoryRequest {
    
    private UUID userId;
    private UUID groupId;
    private String stockName;
    private UUID stockId;
    private String side; // "BUY" or "SELL"
    private Float quantity;
    private Float price;
    private String reason;
}
