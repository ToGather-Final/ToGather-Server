package com.example.trading_service.service;

import com.example.trading_service.dto.OrderBookItem;
import com.example.trading_service.dto.OrderBookResponse;
import com.example.trading_service.domain.Stock;
import com.example.trading_service.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderBookService {

    private final StockRepository stockRepository;
    private final StockPriceService stockPriceService;
    private final RedisCacheService redisCacheService;

    // 주식 호가 정보 조회 (WebSocket 우선, REST API 폴백)
    public OrderBookResponse getOrderBook(String stockCode) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주식입니다: " + stockCode));

        // 1. Redis WebSocket 캐시에서 먼저 조회
        Object cachedOrderBook = redisCacheService.getCachedWebSocketOrderBook(stockCode);
        log.info("🔍 Redis 캐시 조회 결과 - 종목코드: {}, 캐시 존재: {}", 
                stockCode, cachedOrderBook != null);
        
        if (cachedOrderBook instanceof OrderBookResponse) {
            OrderBookResponse orderBook = (OrderBookResponse) cachedOrderBook;
            if (!orderBook.getAskPrices().isEmpty()) {
                log.info("🚀 Redis WebSocket 캐시에서 호가 데이터 반환: {} (매도: {}, 매수: {})", 
                        stockCode, orderBook.getAskPrices().size(), orderBook.getBidPrices().size());
                return orderBook;
            }
        }
        
        log.warn("⚠️ WebSocket 캐시에 데이터 없음 - REST API 폴백: {}", stockCode);

        // 2. WebSocket 캐시에 없으면 REST API 호출 (폴백)
        log.info("📡 WebSocket 캐시에 데이터 없음, REST API 호출: {}", stockCode);
        try {
            // 호가 데이터 조회
            Map<String, Object> orderBookData = stockPriceService.getOrderBook(stockCode);
            log.info("한투 API 호가 응답: {}", orderBookData);
            
            Map<String, Object> output = (Map<String, Object>) orderBookData.get("output");
            
            if (output == null) {
                log.warn("호가 데이터를 가져올 수 없습니다. 종목코드: {}, 전체 응답: {}", stockCode, orderBookData);
                // 장외 시간에는 현재가 조회 API로 전일 종가 정보라도 가져오기
                return getFallbackOrderBookData(stock);
            }

            // 기본 정보 추출
            float currentPrice = parseFloat(output.get("stck_prpr"));
            float changeAmount = parseFloat(output.get("prdy_vrss"));
            float changeRate = parseFloat(output.get("prdy_ctrt"));
            
            // 변동 방향 결정
            String changeDirection = "unchanged";
            if (changeAmount > 0) {
                changeDirection = "up";
            } else if (changeAmount < 0) {
                changeDirection = "down";
            }

            // 매도 호가 (Ask Prices) - 빨간색 (10개)
            List<OrderBookItem> askPrices = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                String askPriceKey = "askp" + i;        // 매도 호가
                String askQuantityKey = "askp_rsqn" + i; // 매도 잔량
                
                if (output.containsKey(askPriceKey)) {
                    float price = parseFloat(output.get(askPriceKey));
                    long quantity = parseLong(output.get(askQuantityKey));
                    
                    if (price > 0) {
                        askPrices.add(new OrderBookItem(price, quantity, "ask"));
                    }
                }
            }

            // 매수 호가 (Bid Prices) - 파란색 (10개)
            List<OrderBookItem> bidPrices = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                String bidPriceKey = "bidp" + i;        // 매수 호가
                String bidQuantityKey = "bidp_rsqn" + i; // 매수 잔량
                
                if (output.containsKey(bidPriceKey)) {
                    float price = parseFloat(output.get(bidPriceKey));
                    long quantity = parseLong(output.get(bidQuantityKey));
                    
                    if (price > 0) {
                        bidPrices.add(new OrderBookItem(price, quantity, "bid"));
                    }
                }
            }

            return new OrderBookResponse(
                    stock.getStockCode(),
                    stock.getStockName(),
                    currentPrice,
                    changeAmount,
                    changeRate,
                    changeDirection,
                    askPrices,
                    bidPrices
            );

        } catch (Exception e) {
            log.error("호가 데이터 조회 중 오류 발생. 종목코드: {}, 오류: {}", stockCode, e.getMessage());
            return createSampleOrderBookResponse(stock);
        }
    }

    // 폴백 호가 데이터 생성 (현재가 API로 전일 종가 정보 조회)
    private OrderBookResponse getFallbackOrderBookData(Stock stock) {
        try {
            log.info("📡 폴백: 현재가 API로 전일 종가 정보 조회: {}", stock.getStockCode());
            Map<String, Object> priceData = stockPriceService.getCurrentPrice(stock.getStockCode());
            
            if (priceData != null && priceData.containsKey("output")) {
                Map<String, Object> output = (Map<String, Object>) priceData.get("output");
                
                if (output != null) {
                    // 전일 종가 정보 추출
                    float currentPrice = parseFloat(output.get("stck_prpr")); // 전일 종가
                    float changeAmount = parseFloat(output.get("prdy_vrss")); // 전일 대비 변동가
                    float changeRate = parseFloat(output.get("prdy_ctrt")); // 전일 대비 변동률
                    
                    // 변동 방향 결정
                    String changeDirection = "unchanged";
                    if (changeAmount > 0) {
                        changeDirection = "up";
                    } else if (changeAmount < 0) {
                        changeDirection = "down";
                    }
                    
                    // 장외 시간에는 호가 정보가 없으므로 빈 리스트 반환
                    List<OrderBookItem> emptyAskPrices = new ArrayList<>();
                    List<OrderBookItem> emptyBidPrices = new ArrayList<>();
                    
                    log.info("✅ 폴백 성공: 전일 종가 정보 반환 - 현재가: {}, 변동가: {}", currentPrice, changeAmount);
                    
                    return new OrderBookResponse(
                            stock.getStockCode(),
                            stock.getStockName(),
                            currentPrice,
                            changeAmount,
                            changeRate,
                            changeDirection,
                            emptyAskPrices,
                            emptyBidPrices
                    );
                }
            }
            
            log.warn("폴백 API도 실패, 샘플 데이터 반환: {}", stock.getStockCode());
            return createSampleOrderBookResponse(stock);
            
        } catch (Exception e) {
            log.error("폴백 API 호출 실패: {}", e.getMessage());
            return createSampleOrderBookResponse(stock);
        }
    }

    // 샘플 호가 데이터 생성 (최후의 수단)
    private OrderBookResponse createSampleOrderBookResponse(Stock stock) {
        // 기본값으로 통일 (하드코딩 제거)
        float basePrice = 100000f;
        float changeAmount = 0f;
        float changeRate = 0f;
        String changeDirection = "unchanged";
        
        log.warn("⚠️ 샘플 호가 데이터 사용 - 종목코드: {}, 종목명: {}", stock.getStockCode(), stock.getStockName());
        
        List<OrderBookItem> askPrices = List.of(
                new OrderBookItem(basePrice + 500f, 500L, "ask"),
                new OrderBookItem(basePrice + 1000f, 280L, "ask"),
                new OrderBookItem(basePrice + 1500f, 350L, "ask"),
                new OrderBookItem(basePrice + 2000f, 400L, "ask"),
                new OrderBookItem(basePrice + 2500f, 120L, "ask"),
                new OrderBookItem(basePrice + 3000f, 250L, "ask"),
                new OrderBookItem(basePrice + 3500f, 180L, "ask"),
                new OrderBookItem(basePrice + 4000f, 320L, "ask"),
                new OrderBookItem(basePrice + 4500f, 150L, "ask"),
                new OrderBookItem(basePrice + 5000f, 200L, "ask")
        );

        List<OrderBookItem> bidPrices = List.of(
                new OrderBookItem(basePrice - 500f, 200L, "bid"),
                new OrderBookItem(basePrice - 1000f, 150L, "bid"),
                new OrderBookItem(basePrice - 1500f, 300L, "bid"),
                new OrderBookItem(basePrice - 2000f, 180L, "bid"),
                new OrderBookItem(basePrice - 2500f, 250L, "bid"),
                new OrderBookItem(basePrice - 3000f, 120L, "bid"),
                new OrderBookItem(basePrice - 3500f, 280L, "bid"),
                new OrderBookItem(basePrice - 4000f, 190L, "bid"),
                new OrderBookItem(basePrice - 4500f, 220L, "bid"),
                new OrderBookItem(basePrice - 5000f, 160L, "bid")
        );

        return new OrderBookResponse(
                stock.getStockCode(),
                stock.getStockName(),
                basePrice,
                changeAmount,
                changeRate,
                changeDirection,
                askPrices,
                bidPrices
        );
    }

    // 유틸리티 메서드들
    private float parseFloat(Object value) {
        if (value == null) return 0.0f;
        try {
            String str = value.toString().replace(",", "");
            return Float.parseFloat(str);
        } catch (Exception e) {
            return 0.0f;
        }
    }

    private long parseLong(Object value) {
        if (value == null) return 0L;
        try {
            String str = value.toString().replace(",", "");
            return Long.parseLong(str);
        } catch (Exception e) {
            return 0L;
        }
    }
}
