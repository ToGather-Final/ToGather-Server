package com.example.trading_service.controller;

import com.example.module_common.dto.InvestmentAccountDto;

import com.example.module_common.dto.TransferToPayResponse;
import com.example.trading_service.domain.InvestmentAccount;
import com.example.trading_service.domain.Order;
import com.example.trading_service.service.*;
import com.example.trading_service.dto.*;
import com.example.trading_service.exception.BusinessException;
import com.example.module_common.dto.vote.VoteTradingRequest;
import com.example.module_common.dto.vote.VoteTradingResponse;
import com.example.trading_service.util.AccountNumberGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/trading")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "투자 거래", description = "주식 매매, 포트폴리오 관리, 계좌 관리 관련 API")
public class TradingController {

    private final TradingService tradingService;
    private final OrderService orderService;
    private final PortfolioCalculationService portfolioCalculationService;
    private final OrderBookService orderBookService;
    private final ChartService chartService;
    private final GroupTradingService groupTradingService;
    private final TradeExecutionService tradeExecutionService;

    @Operation(summary = "투자 계좌 개설", description = "사용자의 투자 계좌를 새로 개설합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "투자 계좌 개설 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/account/invest")
    public ResponseEntity<ApiResponse<UUID>> createInvestmentAccount(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        UUID accountId = tradingService.createInvestmentAccount(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("투자 계좌가 성공적으로 개설되었습니다", accountId));
    }

    @Operation(summary = "주식 매수", description = "지정된 종목을 매수 주문합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "매수 주문 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 잔고 부족"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/trade/buy")
    public ResponseEntity<ApiResponse<String>> buyStock(
            @Parameter(description = "매수 요청 데이터", required = true) @Valid @RequestBody BuyRequest request, 
            Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        orderService.buyStock(userId, request);
        return ResponseEntity.ok(ApiResponse.success("매수 주문이 완료되었습니다"));
    }

    @Operation(summary = "주식 매도", description = "보유한 주식을 매도 주문합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "매도 주문 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 보유 수량 부족"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/trade/sell")
    public ResponseEntity<ApiResponse<String>> sellStock(
            @Parameter(description = "매도 요청 데이터", required = true) @Valid @RequestBody SellRequest request, 
            Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        orderService.sellStock(userId, request);
        return ResponseEntity.ok(ApiResponse.success("매도 주문이 완료되었습니다"));
    }

    // 예수금 충전
    @PutMapping("/trade/deposit")
    public ResponseEntity<ApiResponse<String>> depositFunds(@Valid @RequestBody DepositRequest request,
                                                          Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        tradingService.depositFunds(userId, request);
        return ResponseEntity.ok(ApiResponse.success("예수금 충전이 완료되었습니다"));
    }

    // Internal 예수금 충전 (서비스 간 통신용)
    @PostMapping("/internal/deposit")
    public ResponseEntity<ApiResponse<String>> internalDepositFunds(@Valid @RequestBody InternalDepositRequest request) {
        tradingService.internalDepositFunds(request);
        return ResponseEntity.ok(ApiResponse.success("Internal 예수금 충전이 완료되었습니다"));
    }

    // Internal 투표 기반 거래 실행 (서비스 간 통신용)
    @PostMapping("/internal/vote-trading")
    public ResponseEntity<VoteTradingResponse> executeVoteBasedTrading(@Valid @RequestBody VoteTradingRequest request) {
        VoteTradingResponse response = tradingService.executeVoteBasedTrading(request);
        return ResponseEntity.ok(response);
    }

    // Internal 그룹 예수금 총합 조회 (서비스 간 통신용)
    @PostMapping("/internal/group-balance")
    public ResponseEntity<Integer> getGroupTotalBalance(@RequestBody List<UUID> memberIds) {
        Integer totalBalance = tradingService.getGroupTotalBalance(memberIds);
        return ResponseEntity.ok(totalBalance);
    }

    @Operation(summary = "보유 종목 조회", description = "사용자가 보유한 모든 종목의 실시간 가격 정보를 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "보유 종목 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/portfolio/holdings")
    public ResponseEntity<ApiResponse<List<HoldingResponse>>> getHoldings(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        
        try {
            List<HoldingResponse> holdings = portfolioCalculationService.calculateUserHoldingsWithCache(userId);
            return ResponseEntity.ok(ApiResponse.success(holdings));
        } catch (BusinessException e) {
            // 계좌가 없으면 자동으로 생성
            tradingService.createInvestmentAccount(userId);
            List<HoldingResponse> holdings = portfolioCalculationService.calculateUserHoldingsWithCache(userId);
            return ResponseEntity.ok(ApiResponse.success(holdings));
        }
    }

    // 보유 종목 조회 (StockResponse 형식)
    @GetMapping("/portfolio/stocks")
    public ResponseEntity<ApiResponse<List<StockResponse>>> getPortfolioStocks(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        
        try {
            List<StockResponse> stocks = tradingService.getPortfolioStocks(userId);
            return ResponseEntity.ok(ApiResponse.success(stocks));
        } catch (BusinessException e) {
            // 계좌가 없으면 자동으로 생성
            tradingService.createInvestmentAccount(userId);
            List<StockResponse> stocks = tradingService.getPortfolioStocks(userId);
            return ResponseEntity.ok(ApiResponse.success(stocks));
        }
    }

    // 그룹 포트폴리오 요약 정보 조회
    @GetMapping("/portfolio/summary")
    public ResponseEntity<ApiResponse<PortfolioSummaryResponse>> getGroupPortfolioSummary(
            @RequestParam UUID groupId) {
        PortfolioSummaryResponse summary = groupTradingService.calculateGroupPortfolioSummary(groupId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }



    // 계좌 잔고 조회
    @GetMapping("/account/balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getAccountBalance(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        BalanceResponse balance = portfolioCalculationService.calculateAccountBalance(userId);
        return ResponseEntity.ok(ApiResponse.success(balance));
    }

    // 거래 내역 조회
    @GetMapping("/portfolio/history")
    public ResponseEntity<ApiResponse<List<TradeHistoryResponse>>> getTradeHistory(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        List<TradeHistoryResponse> history = tradingService.getTradeHistory(userId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @Operation(summary = "주식 목록 조회", description = "전체 주식 목록을 조회합니다. 검색어가 있으면 해당 종목을 필터링합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주식 목록 조회 성공")
    })
    @GetMapping("/stocks")
    public ResponseEntity<ApiResponse<List<StockResponse>>> getStocks(
            @Parameter(description = "검색어 (종목명 또는 종목코드)") @RequestParam(required = false) String search) {
        List<StockResponse> stocks = tradingService.getStocks(search);
        return ResponseEntity.ok(ApiResponse.success(stocks));
    }

    // 주식 기본 정보 조회 (현재가, 변동률, 거래량 등) - 간단한 경로
    @GetMapping("/stocks/{stockCode}")
    public ResponseEntity<ApiResponse<StockInfoResponse>> getStockByCode(@PathVariable String stockCode) {
        StockInfoResponse info = tradingService.getStockInfoByCode(stockCode);
        return ResponseEntity.ok(ApiResponse.success(info));
    }

    // 주식 기본 정보 조회 (현재가, 변동률, 거래량 등) - 상세 경로
    @GetMapping("/stocks/{stockCode}/info")
    public ResponseEntity<ApiResponse<StockInfoResponse>> getStockInfoByCode(@PathVariable String stockCode) {
        StockInfoResponse info = tradingService.getStockInfoByCode(stockCode);
        return ResponseEntity.ok(ApiResponse.success(info));
    }

    // 주식 호가 정보 조회
    @GetMapping("/stocks/{stockCode}/orderbook")
    public ResponseEntity<ApiResponse<OrderBookResponse>> getOrderBook(@PathVariable String stockCode) {
        OrderBookResponse orderBook = orderBookService.getOrderBook(stockCode);
        return ResponseEntity.ok(ApiResponse.success(orderBook));
    }

    // 주식 차트 데이터 조회 (캔들차트 + 이동평균선 + 거래량 + 기본 정보)
    @GetMapping("/stocks/{stockCode}/chart")
    public ResponseEntity<ApiResponse<StockInfoResponse>> getStockChart(@PathVariable String stockCode,
                                                                        @RequestParam(defaultValue = "D") String periodDiv) {
        StockInfoResponse chartInfo = tradingService.getStockChartWithInfo(stockCode, periodDiv);
        return ResponseEntity.ok(ApiResponse.success(chartInfo));
    }

    // 전체 주문 조회 (모든 상태)
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        List<OrderResponse> orders = orderService.getAllOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
    
    // 대기 중인 주문 조회 (PENDING)
    @GetMapping("/orders/pending")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getPendingOrders(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        List<OrderResponse> orders = orderService.getPendingOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
    
    // 체결 완료된 주문 조회 (FILLED)
    @GetMapping("/orders/filled")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getFilledOrders(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        List<OrderResponse> orders = orderService.getFilledOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    // 주문 취소
    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<String>> cancelOrder(@PathVariable UUID orderId, 
                                                          Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        orderService.cancelOrder(userId, orderId);
        return ResponseEntity.ok(ApiResponse.success("주문이 취소되었습니다"));
    }

    // 계좌 정보 조회
    @GetMapping("/account/info")
    public ResponseEntity<ApiResponse<AccountInfoResponse>> getAccountInfo(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        AccountInfoResponse accountInfo = tradingService.getAccountInfo(userId);
        return ResponseEntity.ok(ApiResponse.success(accountInfo));
    }

    // 계좌 정보 조회 (마스킹된 계좌번호)
    @GetMapping("/account/info/masked")
    public ResponseEntity<ApiResponse<AccountInfoMaskedResponse>> getAccountInfoMasked(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        AccountInfoResponse accountInfo = tradingService.getAccountInfo(userId);
        
        if (!accountInfo.isHasAccount()) {
            return ResponseEntity.ok(ApiResponse.success(
                new AccountInfoMaskedResponse(null, null, accountInfo.getUserId(), null, false)
            ));
        }
        
        String maskedAccountNo = AccountNumberGenerator.maskAccountNumber(accountInfo.getAccountNo());
        AccountInfoMaskedResponse maskedResponse = new AccountInfoMaskedResponse(
            accountInfo.getAccountId(),
            maskedAccountNo,
            accountInfo.getUserId(),
            accountInfo.getCreatedAt(),
            true
        );
        
        return ResponseEntity.ok(ApiResponse.success(maskedResponse));
    }

    // 특정 종목 거래 내역 조회
    @GetMapping("/portfolio/history/{stockCode}")
    public ResponseEntity<ApiResponse<List<TradeHistoryResponse>>> getStockTradeHistory(@PathVariable String stockCode,
                                                                                       Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        List<TradeHistoryResponse> history = tradingService.getStockTradeHistory(userId, stockCode);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/internal/accounts/user/{userId}")
    @Operation(summary = "사용자별 투자 계좌 조회 (Internal)", description = "사용자 ID로 투자 계좌 정보를 조회합니다.")
    public InvestmentAccountDto getAccountByUserId(@PathVariable UUID userId) {
        return tradingService.getAccountByUserIdInternal(userId);
    }

    @PostMapping("/internal/accounts/create")
    @Operation(summary = "투자 계좌 생성 (Internal)", description = "그룹 참여 시 자동으로 투자 계좌를 생성합니다.")
    public ResponseEntity<ApiResponse<UUID>> createInvestmentAccountInternal(@RequestHeader("X-User-Id") UUID userId) {
        UUID accountId = tradingService.createInvestmentAccount(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("투자 계좌가 성공적으로 개설되었습니다", accountId));
    }


    @PostMapping("/internal/transfer-to-pay")
    @Operation(summary = "투자계좌에서 페이계좌로 송금 (Internal)", description = "서비스 간 통신용 송금 API")
    public ResponseEntity<TransferToPayResponse> internalTransferToPay(
            @Parameter(description = "사용자 ID", required = true) @RequestParam UUID userId,
            @Parameter(description = "송금 금액", required = true) @RequestParam Long amount,
            @Parameter(description = "Transfer ID", required = true) @RequestParam UUID transferId
    ) {
        log.info("Internal 투자계좌에서 페이계좌로 송금: userId={}, amount={}, transferId={}", userId, amount, transferId);

        TransferToPayResponse response = tradingService.transferToPay(userId, amount, transferId);
        return ResponseEntity.ok(response);
    }
  
    // 테스트용: 그룹의 모든 대기 중인 주문을 강제 체결
    @PostMapping("/internal/orders/execute-all")
    @Operation(summary = "그룹의 대기 중인 주문 강제 체결 (테스트용)", description = "그룹의 모든 PENDING 주문을 즉시 체결합니다.")
    public ResponseEntity<ApiResponse<String>> executeAllPendingOrdersForGroup(@RequestParam UUID groupId) {
        log.info("🔥 그룹 강제 체결 요청 - 그룹ID: {}", groupId);
        
        try {
            // 1. 그룹 멤버들의 계좌 조회
            List<InvestmentAccount> groupMembers = groupTradingService.getGroupMembers(groupId);
            log.info("📋 그룹 멤버 수: {}", groupMembers.size());
            
            int totalExecutedCount = 0;
            Map<UUID, Float> stockQuantityMap = new HashMap<>(); // 주식별 총 수량 집계
            Map<UUID, Float> stockPriceMap = new HashMap<>();     // 주식별 가격 (평균 계산용)
            
            // 2. 각 멤버의 대기 중인 주문 조회 및 체결
            for (InvestmentAccount memberAccount : groupMembers) {
                List<Order> pendingOrders = orderService.getPendingOrdersByAccountId(
                    memberAccount.getInvestmentAccountId()
                );
                
                log.info("👤 사용자 {} - 대기 중인 주문: {}건", 
                    memberAccount.getUserId(), pendingOrders.size());
                
                // 3. 각 주문을 강제 체결
                for (Order order : pendingOrders) {
                    try {
                        // 로그용 정보를 미리 가져오기 (트랜잭션 안에서)
                        UUID orderId = order.getOrderId();
                        UUID stockId = order.getStock().getId();
                        float quantity = order.getQuantity();
                        float price = order.getPrice();
                        
                        // 주문가로 즉시 체결
                        tradeExecutionService.executeTrade(order, price);
                        totalExecutedCount++;
                        
                        // 주식별 수량 집계
                        stockQuantityMap.merge(stockId, quantity, Float::sum);
                        stockPriceMap.put(stockId, price);
                        
                        log.info("✅ 주문 체결 완료 - 주문ID: {}, 수량: {}, 가격: {}", 
                            orderId, quantity, price);
                    } catch (Exception e) {
                        log.error("❌ 주문 체결 실패 - 주문ID: {} - {}", 
                            order.getOrderId(), e.getMessage());
                    }
                }
            }
            
            // 4. 그룹 보유량 업데이트 (GroupHoldingCache)
            for (Map.Entry<UUID, Float> entry : stockQuantityMap.entrySet()) {
                UUID stockId = entry.getKey();
                float totalQuantity = entry.getValue();
                float price = stockPriceMap.get(stockId);
                
                try {
                    groupTradingService.updateGroupHoldingAfterTrade(
                        groupId, stockId, totalQuantity, price, groupMembers.size()
                    );
                    log.info("📊 그룹 보유량 업데이트 - 종목ID: {}, 수량: {}", stockId, totalQuantity);
                } catch (Exception e) {
                    log.error("❌ 그룹 보유량 업데이트 실패 - 종목ID: {} - {}", stockId, e.getMessage());
                }
            }
            
            String message = String.format("그룹의 대기 중인 주문 %d건이 강제 체결되었습니다", totalExecutedCount);
            log.info("🎉 {}", message);
            return ResponseEntity.ok(ApiResponse.success(message));
            
        } catch (Exception e) {
            log.error("❌ 그룹 주문 강제 체결 실패 - 그룹ID: {} - {}", groupId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("그룹 주문 강제 체결 실패: " + e.getMessage(), "EXECUTION_FAILED"));
        }
    }

    // 헬퍼 메서드: 인증에서 사용자 ID 추출
    private UUID getUserIdFromAuthentication(Authentication authentication) {
        try {
            if (authentication != null && authentication.getName() != null && !authentication.getName().equals("anonymousUser")) {
                log.debug("인증된 사용자 ID: {}", authentication.getName());
                return UUID.fromString(authentication.getName());
            }
        } catch (IllegalArgumentException e) {
            log.warn("사용자 ID를 UUID로 변환할 수 없습니다: {} - 테스트 UUID 사용", authentication.getName());
        }
        // 테스트용 기본 UUID (실제 운영에서는 제거해야 함)
        log.debug("테스트용 기본 UUID 사용: 550e8400-e29b-41d4-a716-446655440000");
        return UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    }
}