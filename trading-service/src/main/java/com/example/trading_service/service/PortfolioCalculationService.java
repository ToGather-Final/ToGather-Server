package com.example.trading_service.service;

import com.example.trading_service.domain.*;
import com.example.trading_service.dto.*;
import com.example.trading_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PortfolioCalculationService {

    private final HoldingCacheRepository holdingCacheRepository;
    private final InvestmentAccountRepository investmentAccountRepository;
    private final StockRepository stockRepository;
    private final StockPriceService stockPriceService;
    private final BalanceCacheRepository balanceCacheRepository;
    private final RedisCacheService redisCacheService;

    /**
     * 캐싱이 적용된 사용자 보유 주식 조회
     */
    public List<HoldingResponse> calculateUserHoldingsWithCache(UUID userId) {
        // 1. Redis 캐시에서 조회
        @SuppressWarnings("unchecked")
        List<HoldingResponse> cachedHoldings = (List<HoldingResponse>) redisCacheService.getCachedUserHoldings(userId);
        if (cachedHoldings != null) {
            log.debug("사용자 보유 주식 캐시 히트 - 사용자ID: {}", userId);
            return cachedHoldings;
        }

        // 2. 캐시에 없으면 DB에서 조회
        List<HoldingResponse> holdings = calculateUserHoldings(userId);
        
        // 3. Redis에 캐싱
        redisCacheService.cacheUserHoldings(userId, holdings);
        
        return holdings;
    }

    /**
     * 캐싱이 적용된 사용자 잔고 조회
     */
    public BigDecimal getUserBalanceWithCache(UUID userId) {
        // 1. Redis 캐시에서 조회
        BigDecimal cachedBalance = redisCacheService.getCachedUserBalance(userId);
        if (cachedBalance != null) {
            log.debug("사용자 잔고 캐시 히트 - 사용자ID: {}", userId);
            return cachedBalance;
        }

        // 2. 캐시에 없으면 DB에서 조회
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        BalanceCache balance = balanceCacheRepository
                .findByInvestmentAccount_InvestmentAccountId(account.getInvestmentAccountId())
                .orElseThrow(() -> new RuntimeException("잔고 정보를 찾을 수 없습니다."));
        
        BigDecimal userBalance = BigDecimal.valueOf(balance.getBalance());
        
        // 3. Redis에 캐싱
        redisCacheService.cacheUserBalance(userId, userBalance);
        
        return userBalance;
    }

    /**
     * 사용자 잔고 캐시 무효화 (거래 후 호출)
     */
    public void evictUserBalanceCache(UUID userId) {
        redisCacheService.evictUserBalance(userId);
    }

    /**
     * 사용자 보유 주식 캐시 무효화 (거래 후 호출)
     */
    public void evictUserHoldingsCache(UUID userId) {
        redisCacheService.evictUserHoldings(userId);
    }

    // 포트폴리오 요약 정보 계산
    public PortfolioSummaryResponse calculatePortfolioSummary(UUID userId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        List<HoldingCache> holdings = holdingCacheRepository
                .findByInvestmentAccount_InvestmentAccountIdAndQuantityGreaterThan(account.getInvestmentAccountId(), 0);
        
        float totalInvested = 0;
        float totalValue = 0;
        
        for (HoldingCache holding : holdings) {
            totalInvested += holding.getAvgCost() * holding.getQuantity();
            totalValue += holding.getEvaluatedPrice() != null ? holding.getEvaluatedPrice() : 0;
        }
        
        float totalProfit = totalValue - totalInvested;
        float totalProfitRate = totalInvested > 0 ? (totalProfit / totalInvested) * 100 : 0;
        
        // 상위 5개 보유 종목
        List<HoldingResponse> topHoldings = holdings.stream()
                .sorted((h1, h2) -> Float.compare(
                    (h2.getEvaluatedPrice() != null ? h2.getEvaluatedPrice() : 0),
                    (h1.getEvaluatedPrice() != null ? h1.getEvaluatedPrice() : 0)
                ))
                .limit(5)
                .map(this::convertToHoldingResponse)
                .collect(Collectors.toList());
        
        return new PortfolioSummaryResponse(
                totalInvested,
                totalValue,
                totalProfit,
                totalProfitRate,
                holdings.size(),
                topHoldings
        );
    }

    // 보유 종목 목록 계산
    public List<HoldingResponse> calculateUserHoldings(UUID userId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        List<HoldingCache> holdings = holdingCacheRepository
                .findByInvestmentAccount_InvestmentAccountIdAndQuantityGreaterThan(account.getInvestmentAccountId(), 0);
        
        return holdings.stream()
                .map(this::convertToHoldingResponse)
                .collect(Collectors.toList());
    }

    // 계좌 잔고 계산
    public BalanceResponse calculateAccountBalance(UUID userId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        BalanceCache balance = balanceCacheRepository.findByInvestmentAccount_InvestmentAccountId(account.getInvestmentAccountId())
                .orElseThrow(() -> new RuntimeException("잔고 정보를 찾을 수 없습니다"));

        // 보유 종목들의 총 평가금액 계산
        List<HoldingCache> holdings = holdingCacheRepository.findByInvestmentAccount_InvestmentAccountId(account.getInvestmentAccountId());
        
        float totalInvested = 0;
        float totalValue = 0;
        
        for (HoldingCache holding : holdings) {
            totalInvested += holding.getAvgCost() * holding.getQuantity();
            totalValue += holding.getEvaluatedPrice() != null ? holding.getEvaluatedPrice() : 0;
        }
        
        float totalProfit = totalValue - totalInvested;
        float totalProfitRate = totalInvested > 0 ? (totalProfit / totalInvested) * 100 : 0;
        
        return new BalanceResponse(
                balance.getBalanceId(),
                balance.getInvestmentAccount().getInvestmentAccountId(),
                (long) balance.getBalance(),
                totalInvested,
                totalValue,
                totalProfit,
                totalProfitRate
        );
    }

    // HoldingResponse 변환 (실시간 가격 정보 포함)
    private HoldingResponse convertToHoldingResponse(HoldingCache holding) {
        // 주식 정보 조회
        Stock stock = holding.getStock();
        
        // 실시간 주식 가격 및 변동률 조회
        float currentPrice = getCurrentStockPrice(stock.getStockCode());
        Map<String, Float> changeInfo = getStockChangeInfo(stock.getStockCode());
        float changeAmount = changeInfo.get("changeAmount");
        float changeRate = changeInfo.get("changeRate");
        
        // 평가금액 및 수익률 계산
        float evaluatedPrice = currentPrice * holding.getQuantity();
        float totalCost = holding.getAvgCost() * holding.getQuantity();
        float profit = evaluatedPrice - totalCost;
        float profitRate = totalCost > 0 ? (profit / totalCost) * 100 : 0;
        
        // 변동 방향 결정
        String changeDirection = "unchanged";
        if (changeAmount > 0) {
            changeDirection = "up";
        } else if (changeAmount < 0) {
            changeDirection = "down";
        }

        return new HoldingResponse(
                holding.getHoldingId(),
                holding.getStock().getId(),
                stock.getStockCode(),
                stock.getStockName(),
                stock.getStockImage(),
                holding.getQuantity(),
                holding.getAvgCost(),
                currentPrice,
                changeAmount,
                changeRate,
                profit,
                evaluatedPrice,
                profitRate,
                changeDirection
        );
    }

    // 실시간 주식 가격 조회
    private float getCurrentStockPrice(String stockCode) {
        try {
            Map<String, Object> priceData = stockPriceService.getCurrentPrice(stockCode);
            
            // 한투 API 응답에서 현재가 추출
            if (priceData != null && priceData.containsKey("output")) {
                Map<String, Object> output = (Map<String, Object>) priceData.get("output");
                if (output != null && output.containsKey("stck_prpr")) {
                    String priceStr = (String) output.get("stck_prpr");
                    return Float.parseFloat(priceStr.replace(",", ""));
                }
            }
            
            log.warn("주식 가격 정보를 가져올 수 없습니다. 종목코드: {}", stockCode);
            return 0.0f;
        } catch (Exception e) {
            log.error("주식 가격 조회 중 오류 발생. 종목코드: {}, 오류: {}", stockCode, e.getMessage());
            return 0.0f;
        }
    }

    // 주식 변동률 정보 조회
    private Map<String, Float> getStockChangeInfo(String stockCode) {
        try {
            Map<String, Object> priceData = stockPriceService.getCurrentPrice(stockCode);
            
            if (priceData != null && priceData.containsKey("output")) {
                Map<String, Object> output = (Map<String, Object>) priceData.get("output");
                if (output != null) {
                    float changeAmount = 0.0f;
                    float changeRate = 0.0f;
                    
                    if (output.containsKey("prdy_vrss")) {
                        String changeAmountStr = (String) output.get("prdy_vrss");
                        changeAmount = Float.parseFloat(changeAmountStr.replace(",", ""));
                    }
                    
                    if (output.containsKey("prdy_vrss_sign")) {
                        String sign = (String) output.get("prdy_vrss_sign");
                        if ("2".equals(sign)) { // 하락
                            changeAmount = -Math.abs(changeAmount);
                        }
                    }
                    
                    if (output.containsKey("prdy_ctrt")) {
                        String changeRateStr = (String) output.get("prdy_ctrt");
                        changeRate = Float.parseFloat(changeRateStr.replace(",", ""));
                        
                        if (changeAmount < 0) {
                            changeRate = -Math.abs(changeRate);
                        }
                    }
                    
                    return Map.of("changeAmount", changeAmount, "changeRate", changeRate);
                }
            }
            
            log.warn("주식 변동률 정보를 가져올 수 없습니다. 종목코드: {}", stockCode);
            return Map.of("changeAmount", 0.0f, "changeRate", 0.0f);
        } catch (Exception e) {
            log.error("주식 변동률 조회 중 오류 발생. 종목코드: {}, 오류: {}", stockCode, e.getMessage());
            return Map.of("changeAmount", 0.0f, "changeRate", 0.0f);
        }
    }

    private InvestmentAccount getInvestmentAccountByUserId(UUID userId) {
        return investmentAccountRepository.findByUserId(userId.toString())
                .orElseThrow(() -> new RuntimeException("투자 계좌를 찾을 수 없습니다"));
    }
}
