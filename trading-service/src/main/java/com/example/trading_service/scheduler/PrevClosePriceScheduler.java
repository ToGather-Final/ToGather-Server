package com.example.trading_service.scheduler;

import com.example.trading_service.domain.Stock;
import com.example.trading_service.dto.StockPriceResponse;
import com.example.trading_service.repository.StockRepository;
import com.example.trading_service.service.RedisCacheService;
import com.example.trading_service.service.StockPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

/**
 * 전일 종가 캐싱 스케줄러
 * - 서버 시작 시 자동으로 전일 종가 캐싱
 * - 장 시작 전 (08:30) 전일 종가를 조회하여 캐싱
 * - 전일 종가는 하루 종일 변하지 않으므로 한 번만 조회하면 됨
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PrevClosePriceScheduler {

    private final StockRepository stockRepository;
    private final StockPriceService stockPriceService;
    private final RedisCacheService redisCacheService;
    
    private volatile boolean initialized = false;

    /**
     * 서버 시작 시 자동으로 전일 종가 캐싱
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!initialized) {
            log.info("🚀 서버 시작 - 전일 종가 자동 캐싱 시작");
            // 5초 대기 후 실행 (다른 초기화 작업 완료 대기)
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    cachePrevClosePrices();
                    initialized = true;
                } catch (Exception e) {
                    log.error("서버 시작 시 전일 종가 캐싱 실패", e);
                }
            }).start();
        }
    }

    /**
     * 매일 08:30에 전일 종가 캐싱
     * (장 시작 전 준비)
     */
    @Scheduled(cron = "0 30 8 * * MON-FRI")
    public void scheduledCachePrevClosePrices() {
        log.info("📅 [스케줄] 전일 종가 캐싱 시작");
        cachePrevClosePrices();
    }

    /**
     * 전일 종가 캐싱 로직
     */
    private void cachePrevClosePrices() {
        log.info("📊 전일 종가 캐싱 작업 시작");
        
        try {
            // 활성화된 종목들 조회
            List<Stock> activeStocks = stockRepository.findByEnabledTrue();
            log.info("📊 {} 개 종목의 전일 종가 캐싱 중...", activeStocks.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (Stock stock : activeStocks) {
                try {
                    // 주식 가격 정보 조회 (전일 종가 포함)
                    StockPriceResponse priceResponse = stockPriceService.getCachedStockPrice(
                        stock.getId(), 
                        stock.getStockCode(), 
                        stock.getPrdtTypeCd()
                    );
                    
                    if (priceResponse != null && priceResponse.getPrevClosePrice() != null) {
                        Float prevClosePrice = priceResponse.getPrevClosePrice().floatValue();
                        
                        // 0보다 큰 값만 캐싱 (fallback 값 제외)
                        if (prevClosePrice > 0) {
                            redisCacheService.cachePrevClosePrice(stock.getStockCode(), prevClosePrice);
                            successCount++;
                            
                            log.info("✅ 전일 종가 캐싱 완료 - {} ({}): {}원", 
                                stock.getStockName(), stock.getStockCode(), prevClosePrice);
                        } else {
                            log.warn("⚠️ 전일 종가가 0 - API 응답 실패 또는 장외 시간 - {} ({})", 
                                stock.getStockName(), stock.getStockCode());
                            failCount++;
                        }
                    } else {
                        log.warn("⚠️ 전일 종가 없음 (null) - {} ({})", 
                            stock.getStockName(), stock.getStockCode());
                        failCount++;
                    }
                    
                    // API 호출 제한 고려 (0.2초 간격)
                    Thread.sleep(200);
                    
                } catch (Exception e) {
                    log.error("❌ 전일 종가 캐싱 실패 - {} ({}): {}", 
                        stock.getStockName(), stock.getStockCode(), e.getMessage());
                    failCount++;
                }
            }
            
            log.info("✅ 전일 종가 캐싱 완료 - 성공: {}, 실패: {}", successCount, failCount);
            
        } catch (Exception e) {
            log.error("❌ 전일 종가 캐싱 중 오류 발생", e);
        }
    }

    /**
     * 수동 실행용 메서드 (테스트/개발용)
     */
    public void cachePrevClosePricesNow() {
        log.info("🔧 전일 종가 즉시 캐싱 시작");
        cachePrevClosePrices();
    }
}

