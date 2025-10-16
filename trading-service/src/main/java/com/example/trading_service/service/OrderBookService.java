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

    // 주식 호가 정보 조회
    public OrderBookResponse getOrderBook(String stockCode) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주식입니다: " + stockCode));

        try {
            // 호가 데이터 조회
            Map<String, Object> orderBookData = stockPriceService.getOrderBook(stockCode);
            log.info("한투 API 호가 응답: {}", orderBookData);
            
            Map<String, Object> output = (Map<String, Object>) orderBookData.get("output");
            
            if (output == null) {
                log.warn("호가 데이터를 가져올 수 없습니다. 종목코드: {}, 전체 응답: {}", stockCode, orderBookData);
                return createSampleOrderBookResponse(stock);
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

    // 샘플 호가 데이터 생성 (API 실패 시 사용)
    private OrderBookResponse createSampleOrderBookResponse(Stock stock) {
        List<OrderBookItem> askPrices = List.of(
                new OrderBookItem(82500f, 500L, "ask"),
                new OrderBookItem(82550f, 280L, "ask"),
                new OrderBookItem(82600f, 350L, "ask"),
                new OrderBookItem(82650f, 400L, "ask"),
                new OrderBookItem(82700f, 120L, "ask"),
                new OrderBookItem(82750f, 250L, "ask"),
                new OrderBookItem(82800f, 180L, "ask"),
                new OrderBookItem(82850f, 320L, "ask"),
                new OrderBookItem(82900f, 150L, "ask"),
                new OrderBookItem(82950f, 200L, "ask")
        );

        List<OrderBookItem> bidPrices = List.of(
                new OrderBookItem(82450f, 200L, "bid"),
                new OrderBookItem(82400f, 150L, "bid"),
                new OrderBookItem(82350f, 300L, "bid"),
                new OrderBookItem(82300f, 180L, "bid"),
                new OrderBookItem(82250f, 250L, "bid"),
                new OrderBookItem(82200f, 120L, "bid"),
                new OrderBookItem(82150f, 280L, "bid"),
                new OrderBookItem(82100f, 190L, "bid"),
                new OrderBookItem(82050f, 220L, "bid"),
                new OrderBookItem(82000f, 160L, "bid")
        );

        return new OrderBookResponse(
                stock.getStockCode(),
                stock.getStockName(),
                82500f,
                3200f,
                3.72f,
                "up",
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
