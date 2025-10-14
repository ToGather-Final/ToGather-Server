package com.example.trading_service.controller;

import com.example.trading_service.service.TradingService;
import com.example.trading_service.service.PortfolioService;
import com.example.trading_service.service.OrderBookService;
import com.example.trading_service.service.ChartService;
import com.example.trading_service.dto.BuyRequest;
import com.example.trading_service.dto.SellRequest;
import com.example.trading_service.dto.DepositRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/trading")
@RequiredArgsConstructor
public class TradingController {

    private final TradingService tradingService;
    private final PortfolioService portfolioService;
    private final OrderBookService orderBookService;
    private final ChartService chartService;

    // 투자 계좌 개설
    @PostMapping("/account/invest")
    public ResponseEntity<UUID> createInvestmentAccount(Authentication authentication) {
        UUID userId = authentication != null ? 
            UUID.fromString(authentication.getName()) : 
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID accountId = tradingService.createInvestmentAccount(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountId);
    }

    // 주식 매수
    @PutMapping("/trade/buy")
    public ResponseEntity<String> buyStock(@RequestBody BuyRequest request, 
                                         Authentication authentication) {
        UUID userId = authentication != null ? 
            UUID.fromString(authentication.getName()) : 
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        tradingService.buyStock(userId, request);
        return ResponseEntity.ok("매수 주문이 완료되었습니다.");
    }

    // 주식 매도
    @PutMapping("/trade/sell")
    public ResponseEntity<String> sellStock(@RequestBody SellRequest request, 
                                          Authentication authentication) {
        UUID userId = authentication != null ? 
            UUID.fromString(authentication.getName()) : 
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        tradingService.sellStock(userId, request);
        return ResponseEntity.ok("매도 주문이 완료되었습니다.");
    }

    // 예수금 충전
    @PutMapping("/trade/deposit")
    public ResponseEntity<String> depositFunds(@RequestBody DepositRequest request, 
                                             Authentication authentication) {
        UUID userId = authentication != null ? 
            UUID.fromString(authentication.getName()) : 
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        tradingService.depositFunds(userId, request);
        return ResponseEntity.ok("예수금 충전이 완료되었습니다.");
    }

    // 보유 종목 조회 (실시간 가격 정보 포함)
    @GetMapping("/portfolio/holdings")
    public ResponseEntity<?> getHoldings(Authentication authentication) {
        UUID userId = authentication != null ? 
            UUID.fromString(authentication.getName()) : 
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        
        try {
            return ResponseEntity.ok(portfolioService.getUserHoldings(userId));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("투자 계좌를 찾을 수 없습니다")) {
                // 계좌가 없으면 자동으로 생성
                tradingService.createInvestmentAccount(userId);
                return ResponseEntity.ok(portfolioService.getUserHoldings(userId));
            }
            throw e;
        }
    }

    // 실시간 포트폴리오 조회 (클라이언트용 - 이미지 형태)
    @GetMapping("/portfolio/realtime")
    public ResponseEntity<?> getRealtimePortfolio(Authentication authentication) {
        // 임시로 테스트용 UUID 사용 (실제로는 authentication에서 가져와야 함)
        UUID userId;
        if (authentication != null) {
            userId = UUID.fromString(authentication.getName());
        } else {
            // 테스트용 - 실제 사용자 ID로 변경 필요
            userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        }
        
        try {
            return ResponseEntity.ok(portfolioService.getUserHoldings(userId));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("투자 계좌를 찾을 수 없습니다")) {
                // 계좌가 없으면 자동으로 생성
                tradingService.createInvestmentAccount(userId);
                return ResponseEntity.ok(portfolioService.getUserHoldings(userId));
            }
            throw e;
        }
    }

    // 포트폴리오 요약 정보 조회
    @GetMapping("/portfolio/summary")
    public ResponseEntity<?> getPortfolioSummary(Authentication authentication) {
        UUID userId = authentication != null ? 
            UUID.fromString(authentication.getName()) : 
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        return ResponseEntity.ok(portfolioService.getPortfolioSummary(userId));
    }

    // 테스트용 샘플 보유 종목 생성
    @PostMapping("/test/sample-holdings")
    public ResponseEntity<String> createSampleHoldings(Authentication authentication) {
        UUID userId = authentication != null ? 
            UUID.fromString(authentication.getName()) : 
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        portfolioService.createSampleHoldings(userId);
        return ResponseEntity.ok("샘플 보유 종목이 생성되었습니다.");
    }

    // 계좌 잔고 조회
    @GetMapping("/account/balance")
    public ResponseEntity<?> getAccountBalance(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(tradingService.getAccountBalance(userId));
    }

    // 거래 내역 조회
    @GetMapping("/portfolio/history")
    public ResponseEntity<?> getTradeHistory(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(tradingService.getTradeHistory(userId));
    }

    // 주식 조회
    @GetMapping("/stocks")
    public ResponseEntity<?> getStocks(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(tradingService.getStocks(search));
    }

    // 주식 코드로 상세 정보 조회 (차트 데이터 포함)
    @GetMapping("/stocks/{stockCode}/detail")
    public ResponseEntity<?> getStockDetailByCode(@PathVariable String stockCode,
                                                  @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(tradingService.getStockDetailByCode(stockCode, days));
    }

    // 주식 호가 정보 조회
    @GetMapping("/stocks/{stockCode}/orderbook")
    public ResponseEntity<?> getOrderBook(@PathVariable String stockCode) {
        return ResponseEntity.ok(orderBookService.getOrderBook(stockCode));
    }

    // 주식 차트 데이터 조회 (캔들차트 + 이동평균선 + 거래량)
    @GetMapping("/stocks/{stockCode}/chart")
    public ResponseEntity<?> getStockChart(@PathVariable String stockCode,
                                         @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(chartService.getStockChart(stockCode, days));
    }

    // 대기 중인 주문 조회
    @GetMapping("/orders/pending")
    public ResponseEntity<?> getPendingOrders(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(tradingService.getPendingOrders(userId));
    }

    // 주문 취소
    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<String> cancelOrder(@PathVariable UUID orderId, 
                                             Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        tradingService.cancelOrder(userId, orderId);
        return ResponseEntity.ok("주문이 취소되었습니다.");
    }

    // 계좌 정보 조회
    @GetMapping("/account/info")
    public ResponseEntity<?> getAccountInfo(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(tradingService.getAccountInfo(userId));
    }

    // 특정 종목 거래 내역 조회
    @GetMapping("/portfolio/history/{stockId}")
    public ResponseEntity<?> getStockTradeHistory(@PathVariable UUID stockId,
                                                 Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(tradingService.getStockTradeHistory(userId, stockId));
    }


}
