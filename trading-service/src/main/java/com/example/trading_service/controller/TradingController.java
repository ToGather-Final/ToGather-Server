package com.example.trading_service.controller;

import com.example.trading_service.service.TradingService;
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

    // 투자 계좌 개설
    @PostMapping("/account/invest")
    public ResponseEntity<UUID> createInvestmentAccount(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        UUID accountId = tradingService.createInvestmentAccount(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountId);
    }

    // 주식 매수
    @PutMapping("/trade/buy")
    public ResponseEntity<String> buyStock(@RequestBody BuyRequest request, 
                                         Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        tradingService.buyStock(userId, request);
        return ResponseEntity.ok("매수 주문이 완료되었습니다.");
    }

    // 주식 매도
    @PutMapping("/trade/sell")
    public ResponseEntity<String> sellStock(@RequestBody SellRequest request, 
                                          Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        tradingService.sellStock(userId, request);
        return ResponseEntity.ok("매도 주문이 완료되었습니다.");
    }

    // 예수금 충전
    @PutMapping("/trade/deposit")
    public ResponseEntity<String> depositFunds(@RequestBody DepositRequest request, 
                                             Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        tradingService.depositFunds(userId, request);
        return ResponseEntity.ok("예수금 충전이 완료되었습니다.");
    }

    // 보유 종목 조회
    @GetMapping("/portfolio/holdings")
    public ResponseEntity<?> getHoldings(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(tradingService.getUserHoldings(userId));
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

    // 특정 주식 상세 조회
    @GetMapping("/stocks/{stockId}")
    public ResponseEntity<?> getStockDetail(@PathVariable UUID stockId) {
        return ResponseEntity.ok(tradingService.getStockDetail(stockId));
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

    // 포트폴리오 요약 정보 조회
    @GetMapping("/portfolio/summary")
    public ResponseEntity<?> getPortfolioSummary(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(tradingService.getPortfolioSummary(userId));
    }

}
