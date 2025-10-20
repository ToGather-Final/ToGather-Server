// user-service/src/main/java/com/example/user_service/client/TradingServiceClient.java

package com.example.user_service.client;

import com.example.module_common.dto.InvestmentAccountDto;
import com.example.user_service.dto.InternalDepositRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(
        name = "trading-service",
        url = "${app.services.trading-service.url:http://localhost:8081}"
)
public interface TradingServiceClient {

    @GetMapping("/trading/internal/accounts/user/{userId}")
    InvestmentAccountDto getAccountByUserId(@PathVariable("userId") UUID userId);

    /**
     * 그룹 멤버들의 투자 계좌 조회
     */
    @GetMapping("/trading/internal/accounts/group/{groupId}")
    List<InvestmentAccountDto> getGroupMemberAccounts(@PathVariable("groupId") UUID groupId);

    /**
     * 투자 계좌 생성 (그룹 참여 시 자동 생성)
     */
    @PostMapping("/trading/internal/accounts/create")
    ResponseEntity<Map<String, Object>> createInvestmentAccount(@RequestHeader("X-User-Id") UUID userId);

    /**
     * 예수금 충전 (그룹 초기 자금 지급)
     * - POST /internal/deposit 사용
     * - InternalDepositRequest: userId, amount, groupId, description
     */
    @PostMapping("/internal/deposit")
    ResponseEntity<Map<String, Object>> depositFunds(@RequestBody InternalDepositRequest request);
}