package com.example.trading_service.service;

import com.example.trading_service.domain.HoldingCache;
import com.example.trading_service.domain.InvestmentAccount;
import com.example.trading_service.domain.Stock;
import com.example.trading_service.dto.HoldingResponse;
import com.example.trading_service.dto.PortfolioSummaryResponse;
import com.example.trading_service.dto.StockPriceResponse;
import com.example.trading_service.repository.HoldingCacheRepository;
import com.example.trading_service.repository.InvestmentAccountRepository;
import com.example.trading_service.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PortfolioService {

    private final HoldingCacheRepository holdingCacheRepository;
    private final InvestmentAccountRepository investmentAccountRepository;
    private final StockRepository stockRepository;
    private final StockPriceService stockPriceService;

    // 보유 종목 조회 (실시간 가격 정보 포함)
    public List<HoldingResponse> getUserHoldings(UUID userId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);

        List<HoldingCache> holdings = holdingCacheRepository
                .findByAccountIdAndQuantityGreaterThan(account.getInvestmentAccountId(), 0);

        return holdings.stream()
                .map(this::convertToHoldingResponse)
                .collect(Collectors.toList());
    }

    // 포트폴리오 요약 정보 조회
    public PortfolioSummaryResponse getPortfolioSummary(UUID userId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);

        List<HoldingCache> holdings = holdingCacheRepository
                .findByAccountIdAndQuantityGreaterThan(account.getInvestmentAccountId(), 0);

        BigDecimal totalInvestedBD = BigDecimal.ZERO;
        BigDecimal totalValueBD = BigDecimal.ZERO;

        for (HoldingCache holding : holdings) {
            BigDecimal avgCost = BigDecimal.valueOf(holding.getAvgCost());
            BigDecimal quantity = BigDecimal.valueOf(holding.getQuantity());
            BigDecimal evaluatedPrice = holding.getEvaluatedPrice() != null ? 
                BigDecimal.valueOf(holding.getEvaluatedPrice()) : BigDecimal.ZERO;
            
            totalInvestedBD = totalInvestedBD.add(avgCost.multiply(quantity));
            totalValueBD = totalValueBD.add(evaluatedPrice);
        }

        BigDecimal totalProfitBD = totalValueBD.subtract(totalInvestedBD);
        BigDecimal totalProfitRateBD = totalInvestedBD.compareTo(BigDecimal.ZERO) > 0 ? 
            totalProfitBD.divide(totalInvestedBD, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : 
            BigDecimal.ZERO;
        
        // 금액은 정수로 반올림, 수익률은 소수점 2자리로 반올림
        float totalInvested = totalInvestedBD.setScale(0, RoundingMode.HALF_UP).floatValue();
        float totalValue = totalValueBD.setScale(0, RoundingMode.HALF_UP).floatValue();
        float totalProfit = totalProfitBD.setScale(0, RoundingMode.HALF_UP).floatValue();
        float totalProfitRate = totalProfitRateBD.setScale(2, RoundingMode.HALF_UP).floatValue();

        // 모든 보유 종목 (평가금액 기준 내림차순 정렬)
        List<HoldingResponse> topHoldings = holdings.stream()
                .sorted((h1, h2) -> Float.compare(
                        (h2.getEvaluatedPrice() != null ? h2.getEvaluatedPrice() : 0),
                        (h1.getEvaluatedPrice() != null ? h1.getEvaluatedPrice() : 0)
                ))
                .map(this::convertToHoldingResponse)
                .collect(Collectors.toList());

        return new PortfolioSummaryResponse(
                totalInvested,
                totalValue,
                totalProfit,
                totalProfitRate,
                holdings.size(),
                topHoldings,
                0.0f // totalCashBalance - 개인 포트폴리오에서는 0으로 설정
        );
    }

    // 테스트용 샘플 보유 종목 생성
    @Transactional
    public void createSampleHoldings(UUID userId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);

        // 기존 보유 종목이 있으면 생성하지 않음
        List<HoldingCache> existingHoldings = holdingCacheRepository
                .findByAccountIdAndQuantityGreaterThan(account.getInvestmentAccountId(), 0);
        if (!existingHoldings.isEmpty()) {
            log.info("이미 보유 종목이 존재합니다. 사용자: {}", userId);
            return;
        }

        // 이미지에 나온 주식들의 샘플 보유 종목 생성
        createSampleHolding(account.getInvestmentAccountId(), "035420", 2.5f, 485500f); // NAVER
        createSampleHolding(account.getInvestmentAccountId(), "035720", 8.333f, 416650f); // 카카오
        createSampleHolding(account.getInvestmentAccountId(), "051910", 1.25f, 625000f); // LG화학
        createSampleHolding(account.getInvestmentAccountId(), "005380", 3.75f, 712500f); // 현대차
        createSampleHolding(account.getInvestmentAccountId(), "000660", 0.875f, 1125000f); // SK하이닉스
        createSampleHolding(account.getInvestmentAccountId(), "005930", 2.0f, 72500f); // 삼성전자

        log.info("샘플 보유 종목이 생성되었습니다. 사용자: {}", userId);
    }

    // HoldingResponse 변환 (실시간 가격 정보 포함) - WebSocket 우선
    private HoldingResponse convertToHoldingResponse(HoldingCache holding) {
        // 주식 정보 조회
        Stock stock = holding.getStock();

        // WebSocket 우선으로 실시간 주식 가격 및 변동률 조회
        StockPriceResponse priceInfo = stockPriceService.getCachedStockPrice(stock.getId(), stock.getStockCode());
        float currentPrice = priceInfo.getCurrentPrice().floatValue();
        float changeAmount = priceInfo.getChangePrice().floatValue();
        float changeRate = priceInfo.getChangeRate();

        // 평가금액 및 수익률 계산 (BigDecimal 사용으로 정밀도 개선)
        BigDecimal currentPriceBD = BigDecimal.valueOf(currentPrice);
        BigDecimal quantityBD = BigDecimal.valueOf(holding.getQuantity());
        BigDecimal avgCostBD = BigDecimal.valueOf(holding.getAvgCost());
        
        BigDecimal evaluatedPriceBD = currentPriceBD.multiply(quantityBD);
        BigDecimal totalCostBD = avgCostBD.multiply(quantityBD);
        BigDecimal profitBD = evaluatedPriceBD.subtract(totalCostBD);
        BigDecimal profitRateBD = totalCostBD.compareTo(BigDecimal.ZERO) > 0 ? 
            profitBD.divide(totalCostBD, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : 
            BigDecimal.ZERO;
        
        // 금액은 정수로 반올림, 수익률은 소수점 2자리로 반올림
        float evaluatedPrice = evaluatedPriceBD.setScale(0, RoundingMode.HALF_UP).floatValue();
        float totalCost = totalCostBD.setScale(0, RoundingMode.HALF_UP).floatValue();
        float profit = profitBD.setScale(0, RoundingMode.HALF_UP).floatValue();
        float profitRate = profitRateBD.setScale(2, RoundingMode.HALF_UP).floatValue();

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
                avgCostBD.setScale(0, RoundingMode.HALF_UP).floatValue(), // 매입금액도 정수로 반올림
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

    private void createSampleHolding(UUID accountId, String stockCode, float quantity, float avgCost) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주식입니다: " + stockCode));

        HoldingCache holding = new HoldingCache();
        InvestmentAccount account = investmentAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("투자 계좌를 찾을 수 없습니다"));

        holding.setInvestmentAccount(account);
        holding.setStock(stock);
        holding.setQuantity(quantity);
        holding.setAvgCost(avgCost);
        holding.setEvaluatedPrice(avgCost * quantity); // 초기값은 평균 매입가로 설정

        holdingCacheRepository.save(holding);
    }

    private InvestmentAccount getInvestmentAccountByUserId(UUID userId) {
        return investmentAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("투자 계좌를 찾을 수 없습니다."));
    }
}
