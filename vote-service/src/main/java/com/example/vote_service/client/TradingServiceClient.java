package com.example.vote_service.client;

import com.example.vote_service.dto.InternalDepositRequest;
import com.example.module_common.dto.vote.VoteTradingRequest;
import com.example.module_common.dto.vote.VoteTradingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

/**
 * Trading Service Client
 * - vote-service에서 trading-service의 internal API 호출용
 */
@FeignClient(
    name = "trading-service",
    url = "${app.services.trading-service.url:http://localhost:8081}"
)
public interface TradingServiceClient {
    
    /**
     * Internal 예수금 충전
     * - PAY 투표 가결 시 그룹 멤버들의 예수금 자동 충전
     */
    @PostMapping("/trading/internal/deposit")
    ResponseEntity<String> internalDepositFunds(@RequestBody InternalDepositRequest request);
    
    /**
     * 투표 기반 거래 실행
     * - TRADE 투표 가결 시 자동 거래 실행
     */
    @PostMapping("/trading/internal/vote-trading")
    VoteTradingResponse executeVoteBasedTrading(@RequestBody VoteTradingRequest request);
    
    /**
     * 그룹 예수금 총합 조회
     * - 그룹 멤버들의 예수금 잔액 합계
     */
    @PostMapping("/trading/internal/group-balance")
    Integer getGroupTotalBalance(@RequestBody List<UUID> memberIds);
}