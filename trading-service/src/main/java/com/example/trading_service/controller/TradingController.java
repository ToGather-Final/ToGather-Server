package com.example.trading_service.controller;

import com.example.trading_service.service.*;
import com.example.trading_service.dto.*;
import com.example.trading_service.exception.BusinessException;
import com.example.trading_service.util.AccountNumberGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trading")
@RequiredArgsConstructor
@Tag(name = "투자 거래", description = "주식 매매, 포트폴리오 관리, 계좌 관리 관련 API")
public class TradingController {

    private final TradingService tradingService;
    private final OrderService orderService;
    private final PortfolioCalculationService portfolioCalculationService;
    private final OrderBookService orderBookService;
    private final ChartService chartService;
    private final GroupTradingService groupTradingService;

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

    // 주식 기본 정보 조회 (현재가, 변동률, 거래량 등)
    @GetMapping("/stocks/{stockCode}")
    public ResponseEntity<ApiResponse<StockInfoResponse>> getStockByCode(@PathVariable String stockCode) {
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

    // 대기 중인 주문 조회
    @GetMapping("/orders/pending")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getPendingOrders(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        List<OrderResponse> orders = orderService.getPendingOrders(userId);
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

    // 헬퍼 메서드: 인증에서 사용자 ID 추출
    private UUID getUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            return UUID.fromString(authentication.getName());
        }
        // 테스트용 기본 UUID (실제 운영에서는 제거해야 함)
        return UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    }
}