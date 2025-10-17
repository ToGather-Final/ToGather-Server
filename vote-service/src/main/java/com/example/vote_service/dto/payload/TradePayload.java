package com.example.vote_service.dto.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * TRADE 카테고리 제안의 payload DTO
 * - 주식 매매 관련 정보를 담음
 * - JSON 필드명과 DB 컬럼명 매핑:
 *   - stockId (JSON) ↔ stock_id (DB)
 *   - stockName (JSON) ↔ stock_name (DB)
 */
public record TradePayload(
        @NotBlank String reason,                    // 제안 이유
        @NotNull @JsonProperty("stockId") UUID stockId,        // 주식 ID (JSON: stockId, DB: stock_id)
        @NotBlank @JsonProperty("stockName") String stockName, // 주식명 (JSON: stockName, DB: stock_name)
        @NotNull @Positive Integer price,           // 주가
        @NotNull @Positive Integer quantity         // 수량
) {
}
