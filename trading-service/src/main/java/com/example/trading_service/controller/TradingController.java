package com.example.trading_service.controller;

import com.example.trading_service.service.*;
import com.example.trading_service.dto.*;
import com.example.trading_service.exception.BusinessException;
import com.example.trading_service.util.AccountNumberGenerator;
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
public class TradingController {

    private final TradingService tradingService;
    private final OrderService orderService;
    private final PortfolioCalculationService portfolioCalculationService;
    private final OrderBookService orderBookService;
    private final ChartService chartService;

    // 투자 계좌 개설
    @PostMapping("/account/invest")
    public ResponseEntity<ApiResponse<UUID>> createInvestmentAccount(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        UUID accountId = tradingService.createInvestmentAccount(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("투자 계좌가 성공적으로 개설되었습니다", accountId));
    }

    // 주식 매수
    @PutMapping("/trade/buy")
    public ResponseEntity<ApiResponse<String>> buyStock(@Valid @RequestBody BuyRequest request, 
                                                       Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        orderService.buyStock(userId, request);
        return ResponseEntity.ok(ApiResponse.success("매수 주문이 완료되었습니다"));
    }

    // 주식 매도
    @PutMapping("/trade/sell")
    public ResponseEntity<ApiResponse<String>> sellStock(@Valid @RequestBody SellRequest request, 
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

    // 보유 종목 조회 (실시간 가격 정보 포함)
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

    // 포트폴리오 요약 정보 조회
    @GetMapping("/portfolio/summary")
    public ResponseEntity<ApiResponse<PortfolioSummaryResponse>> getPortfolioSummary(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        PortfolioSummaryResponse summary = portfolioCalculationService.calculatePortfolioSummary(userId);
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

    // 주식 조회
    @GetMapping("/stocks")
    public ResponseEntity<ApiResponse<List<StockResponse>>> getStocks(@RequestParam(required = false) String search) {
        List<StockResponse> stocks = tradingService.getStocks(search);
        return ResponseEntity.ok(ApiResponse.success(stocks));
    }

    // 주식 기본 정보 조회 (현재가, 변동률, 거래량 등)
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