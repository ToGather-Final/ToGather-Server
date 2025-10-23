package com.example.trading_service.client;

import com.example.trading_service.dto.TradeFailedHistoryRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * VoteService FeignClient
 * - 거래 실패 히스토리 저장을 위해 vote-service와 통신
 */
@FeignClient(
    name = "vote-service",
    url = "${app.services.vote-service.url:http://localhost:8084}"
)
public interface VoteServiceClient {

    /**
     * 거래 실패 히스토리 저장
     * POST /internal/history/trade-failed
     */
    @PostMapping("/internal/history/trade-failed")
    void saveTradeFailedHistory(TradeFailedHistoryRequest request);
}
