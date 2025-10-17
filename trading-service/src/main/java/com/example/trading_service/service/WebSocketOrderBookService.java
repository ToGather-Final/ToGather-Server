package com.example.trading_service.service;

import com.example.trading_service.dto.OrderBookItem;
import com.example.trading_service.dto.OrderBookResponse;
import com.example.trading_service.repository.StockRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketOrderBookService {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisCacheService redisCacheService;
    @Lazy
    private final TradeExecutionService tradeExecutionService;
    private final StockRepository stockRepository;

    // 종목명 캐시 (성능 최적화를 위해)
    private final Map<String, String> stockNameCache = new HashMap<>();

    /**
     * 한투 WebSocket 메시지를 파싱하고 브로드캐스트
     */
    public void handleOrderBookMessage(String message) {
        try {
            log.info("📨 호가 메시지 수신: {}", message);
            
            // 실시간 데이터인지 확인 (|로 구분되는 형식)
            if (message.contains("|")) {
                log.info("📊 실시간 파이프 구분 데이터 수신");
                handleRealtimeData(message);
                return;
            }
            
            // ^ 구분자 형식 메시지 처리 (호가 데이터)
            if (message.contains("^")) {
                log.info("📊 실시간 ^ 구분자 데이터 수신");
                // | 구분자로 시작하는 메시지에서 종목코드 추출
                String stockCode = extractStockCodeFromMessage(message);
                handleOrderBookData(message, stockCode);
                return;
            }
            
            // JSON 형식 메시지 처리
            JsonNode rootNode = objectMapper.readTree(message);
            
            // PINGPONG 메시지 처리
            if (isPingPongMessage(rootNode)) {
                log.info("💓 PINGPONG 하트비트 메시지 수신 - 연결 유지 중");
                return;
            }
            
            // 연결 확인 메시지인지 체크
            if (isConnectionMessage(rootNode)) {
                log.info("🔗 WebSocket 연결 확인 메시지 수신");
                return;
            }
            
            // 호가 데이터 파싱
            OrderBookResponse orderBook = parseOrderBookData(rootNode);
            if (orderBook != null) {
                log.info("📊 호가 데이터 파싱 성공: {}", orderBook.getStockCode());
                
                // 1. Redis 캐시에 저장 (30초 TTL)
                redisCacheService.cacheWebSocketOrderBook(orderBook.getStockCode(), orderBook);
                log.info("💾 Redis에 호가 데이터 캐시 저장: {}", orderBook.getStockCode());
                
                // 2. 클라이언트에게 브로드캐스트
                broadcastOrderBook(orderBook);
            } else {
                log.warn("⚠️ 호가 데이터 파싱 실패 - null 반환");
            }
            
        } catch (Exception e) {
            log.error("❌ 호가 메시지 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * PINGPONG 메시지인지 확인
     */
    private boolean isPingPongMessage(JsonNode rootNode) {
        try {
            JsonNode header = rootNode.get("header");
            if (header != null) {
                String trId = header.get("tr_id").asText();
                return "PINGPONG".equals(trId);
            }
        } catch (Exception e) {
            // PINGPONG 메시지가 아닌 경우 무시
        }
        return false;
    }

    /**
     * 연결 확인 메시지인지 체크 (한투 API 문서 기준)
     */
    private boolean isConnectionMessage(JsonNode rootNode) {
        try {
            JsonNode header = rootNode.get("header");
            if (header != null) {
                String trId = header.get("tr_id").asText();
                // 연결 확인 관련 TR_ID들
                return "H0STCNT0".equals(trId) || "H0STCNI0".equals(trId);
            }
        } catch (Exception e) {
            // 연결 확인 메시지가 아닌 경우 무시
        }
        return false;
    }

    /**
     * 실시간 파이프 구분 데이터 처리 (한투 API 문서 기준)
     * 형식: 암호화유무|TR_ID|데이터건수|응답데이터
     */
    private void handleRealtimeData(String message) {
        try {
            String[] parts = message.split("\\|");
            if (parts.length >= 4) {
                String encryptionFlag = parts[0]; // 0: 암호화 안됨, 1: 암호화됨
                String trId = parts[1];
                String dataCount = parts[2];
                String responseData = parts[3];
                
                log.info("📊 실시간 데이터 파싱: 암호화={}, TR_ID={}, 건수={}", 
                    encryptionFlag, trId, dataCount);
                
                if ("0".equals(encryptionFlag) && "H0STASP0".equals(trId)) {
                    // 암호화되지 않은 호가 데이터 처리
                    parseRealtimeOrderBook(responseData);
                } else if ("1".equals(encryptionFlag)) {
                    log.warn("⚠️ 암호화된 데이터 수신 - 복호화 로직 필요");
                } else {
                    log.info("📊 다른 TR_ID 수신: {}", trId);
                }
            } else {
                log.warn("⚠️ 실시간 데이터 형식이 올바르지 않음: {}", message);
            }
        } catch (Exception e) {
            log.error("❌ 실시간 데이터 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 실시간 호가 데이터 파싱 (^로 구분되는 응답 데이터)
     */
    private void parseRealtimeOrderBook(String responseData) {
        try {
            // ^로 구분되는 데이터 파싱
            String[] dataParts = responseData.split("\\^");
            log.info("📈 실시간 호가 데이터: {}개 필드", dataParts.length);
            
            // 디버깅을 위해 처음 10개 필드 출력
            for (int i = 0; i < Math.min(10, dataParts.length); i++) {
                log.info("📊 필드[{}]: {}", i, dataParts[i]);
            }
            
            if (dataParts.length < 5) {
                log.warn("⚠️ 호가 데이터 필드가 부족함: {}", dataParts.length);
                return;
            }
            
            // 첫 번째 필드에서 종목코드 추출 (예: 005930^091217^0^97400^97500...)
            String stockCode = dataParts[0];
            log.info("📊 종목코드: {} ({}개 필드)", stockCode, dataParts.length);
            
            // 호가 데이터 파싱 (간단한 형태)
            List<OrderBookItem> askPrices = new ArrayList<>();
            List<OrderBookItem> bidPrices = new ArrayList<>();
            
            // 한투 API 호가 데이터 형식에 따른 파싱
            // 형식: 종목코드^시간^현재가^매도호가1^매도호가2^...^매도호가10^매수호가1^매수호가2^...^매수호가10^매도수량1^매도수량2^...^매도수량10^매수수량1^매수수량2^...^매수수량10
            
            // 매도 호가 (ASK) - 10개 (인덱스 3~12)
            for (int i = 3; i < 13 && i < dataParts.length; i++) {
                String priceStr = dataParts[i];
                log.info("📊 매도 호가 파싱 [{}]: {}", i, priceStr);
                
                // 0이 아닌 값만 처리
                if (priceStr != null && !priceStr.equals("0") && !priceStr.isEmpty() && !priceStr.equals("")) {
                    try {
                        float price = Float.parseFloat(priceStr);
                        if (price > 0) {
                            // 수량은 나중에 파싱 (인덱스 23~32)
                            long quantity = 0L;
                            if (i + 20 < dataParts.length) {
                                String quantityStr = dataParts[i + 20];
                                if (quantityStr != null && !quantityStr.equals("0") && !quantityStr.isEmpty()) {
                                    try {
                                        quantity = Long.parseLong(quantityStr);
                                    } catch (NumberFormatException e) {
                                        log.warn("⚠️ 매도 수량 파싱 실패: {}", quantityStr);
                                    }
                                }
                            }
                            askPrices.add(new OrderBookItem(price, quantity, "ask"));
                            log.info("✅ 매도 호가 추가: {}원, 수량: {}", price, quantity);
                        } else {
                            log.warn("⚠️ 매도 호가가 0 이하: {}", price);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("⚠️ 매도 호가 파싱 실패: {} - {}", priceStr, e.getMessage());
                    }
                } else {
                    log.info("📊 매도 호가 [{}] 스킵: {}", i, priceStr);
                }
            }
            
            // 매수 호가 (BID) - 10개 (인덱스 13~22)
            for (int i = 13; i < 23 && i < dataParts.length; i++) {
                String priceStr = dataParts[i];
                log.info("📊 매수 호가 파싱 [{}]: {}", i, priceStr);
                
                // 0이 아닌 값만 처리
                if (priceStr != null && !priceStr.equals("0") && !priceStr.isEmpty() && !priceStr.equals("")) {
                    try {
                        float price = Float.parseFloat(priceStr);
                        if (price > 0) {
                            // 수량은 나중에 파싱 (인덱스 33~42)
                            long quantity = 0L;
                            if (i + 20 < dataParts.length) {
                                String quantityStr = dataParts[i + 20];
                                if (quantityStr != null && !quantityStr.equals("0") && !quantityStr.isEmpty()) {
                                    try {
                                        quantity = Long.parseLong(quantityStr);
                                    } catch (NumberFormatException e) {
                                        log.warn("⚠️ 매수 수량 파싱 실패: {}", quantityStr);
                                    }
                                }
                            }
                            bidPrices.add(new OrderBookItem(price, quantity, "bid"));
                            log.info("✅ 매수 호가 추가: {}원, 수량: {}", price, quantity);
                        } else {
                            log.warn("⚠️ 매수 호가가 0 이하: {}", price);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("⚠️ 매수 호가 파싱 실패: {} - {}", priceStr, e.getMessage());
                    }
                } else {
                    log.info("📊 매수 호가 [{}] 스킵: {}", i, priceStr);
                }
            }
            
            // OrderBookResponse 생성
            OrderBookResponse orderBook = new OrderBookResponse(
                stockCode,
                getStockName(stockCode),
                0.0f,  // currentPrice
                0.0f,  // changeAmount
                0.0f,  // changeRate
                "unchanged",  // changeDirection
                askPrices,
                bidPrices
            );
            
            // Redis에 캐시
            redisCacheService.cacheWebSocketOrderBook(stockCode, orderBook);
            
            // WebSocket으로 브로드캐스트
            messagingTemplate.convertAndSend("/topic/orderbook/" + stockCode, orderBook);
            
            // 호가 데이터 업데이트 시 지정가 주문 체결 확인
            checkPendingLimitOrders(stockCode);
            
            log.info("✅ 실시간 호가 데이터 처리 완료: {} (매도: {}, 매수: {})", 
                    stockCode, askPrices.size(), bidPrices.size());
            
            // 호가 데이터가 비어있으면 경고
            if (askPrices.isEmpty() && bidPrices.isEmpty()) {
                log.warn("⚠️ 호가 데이터가 비어있음 - 종목코드: {}, 필드수: {}", stockCode, dataParts.length);
                
                // 디버깅을 위해 모든 필드 출력
                log.warn("🔍 전체 데이터 필드:");
                for (int i = 0; i < Math.min(50, dataParts.length); i++) {
                    log.warn("  [{}]: {}", i, dataParts[i]);
                }
            }
            
        } catch (Exception e) {
            log.error("❌ 실시간 호가 데이터 파싱 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 한투 호가 데이터를 OrderBookResponse로 파싱
     */
    private OrderBookResponse parseOrderBookData(JsonNode rootNode) {
        try {
            JsonNode body = rootNode.get("body");
            if (body == null) {
                log.warn("body가 없는 메시지: {}", rootNode);
                return null;
            }

            // 기본 정보 추출
            String stockCode = extractStockCode(body);
            if (stockCode == null) {
                log.warn("종목코드를 찾을 수 없습니다: {}", body);
                return null;
            }

            String stockName = getStockName(stockCode);
            
            // 현재가 정보 (실제 호가 데이터에서는 제공되지 않을 수 있음)
            Float currentPrice = parseFloat(body.get("stck_prpr"));
            Float changeAmount = parseFloat(body.get("prdy_vrss"));
            Float changeRate = parseFloat(body.get("prdy_ctrt"));
            
            // 변동 방향 결정
            String changeDirection = "unchanged";
            if (changeAmount != null) {
                if (changeAmount > 0) {
                    changeDirection = "up";
                } else if (changeAmount < 0) {
                    changeDirection = "down";
                }
            }

            // 매도 호가 (ASK) - 10개
            List<OrderBookItem> askPrices = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                Float price = parseFloat(body.get("ASKP" + i));
                Long quantity = parseLong(body.get("ASKP_RSQN" + i));
                
                if (price != null && price > 0) {
                    askPrices.add(new OrderBookItem(price, quantity != null ? quantity : 0L, "ask"));
                }
            }

            // 매수 호가 (BID) - 10개
            List<OrderBookItem> bidPrices = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                Float price = parseFloat(body.get("BIDP" + i));
                Long quantity = parseLong(body.get("BIDP_RSQN" + i));
                
                if (price != null && price > 0) {
                    bidPrices.add(new OrderBookItem(price, quantity != null ? quantity : 0L, "bid"));
                }
            }

            return new OrderBookResponse(
                stockCode,
                stockName,
                currentPrice,
                changeAmount,
                changeRate,
                changeDirection,
                askPrices,
                bidPrices
            );

        } catch (Exception e) {
            log.error("호가 데이터 파싱 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 종목코드 추출
     */
    private String extractStockCode(JsonNode body) {
        try {
            JsonNode stockCodeNode = body.get("mksc_shrn_iscd");
        if (stockCodeNode != null) {
            return stockCodeNode.asText();
        }
        
            // 다른 필드명으로 시도
            stockCodeNode = body.get("stck_shrn_iscd");
        if (stockCodeNode != null) {
            return stockCodeNode.asText();
        }
        
        return null;
        } catch (Exception e) {
            log.error("종목코드 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Float 파싱 헬퍼
     */
    private Float parseFloat(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return Float.parseFloat(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Long 파싱 헬퍼
     */
    private Long parseLong(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return Long.parseLong(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 호가 데이터 브로드캐스트
     */
    private void broadcastOrderBook(OrderBookResponse orderBook) {
        try {
            String destination = "/topic/orderbook/" + orderBook.getStockCode();
            messagingTemplate.convertAndSend(destination, orderBook);
            log.info("📤 호가 데이터 브로드캐스트 완료: {}", destination);
        } catch (Exception e) {
            log.error("호가 데이터 브로드캐스트 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * WebSocket 연결 상태 확인
     */
    public boolean isWebSocketConnected() {
        // 실제 구현에서는 WebSocket 연결 상태를 확인
        return true; // 임시로 true 반환
    }

    /**
     * 캐시된 호가 데이터 조회
     */
    public OrderBookResponse getCachedOrderBook(String stockCode) {
        try {
            Object cached = redisCacheService.getCachedWebSocketOrderBook(stockCode);
            if (cached instanceof OrderBookResponse) {
                return (OrderBookResponse) cached;
            }
        } catch (Exception e) {
            log.error("캐시된 호가 데이터 조회 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * WebSocket 캐시 상태 조회
     */
    public Map<String, Object> getCacheStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // DB에서 활성화된 종목들의 캐시 상태 확인
            List<String> activeStockCodes = stockRepository.findByEnabledTrue()
                    .stream()
                    .map(stock -> stock.getStockCode())
                    .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> cacheStatus = new HashMap<>();
            int cachedCount = 0;
            
            for (String stockCode : activeStockCodes) {
                Object cached = redisCacheService.getCachedWebSocketOrderBook(stockCode);
                boolean isCached = cached != null;
                cacheStatus.put(stockCode, isCached);
                if (isCached) {
                    cachedCount++;
                }
            }
            
            status.put("totalStocks", activeStockCodes.size());
            status.put("cachedStocks", cachedCount);
            status.put("cacheDetails", cacheStatus);
            status.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("캐시 상태 조회 실패: {}", e.getMessage());
            status.put("error", e.getMessage());
        }
        
        return status;
    }

    /**
     * WebSocket 메시지에서 종목코드 추출
     * 형식: 0|H0STASP0|001|005930^091217^0^97400^97500...
     */
    private String extractStockCodeFromMessage(String message) {
        try {
            if (message.contains("|")) {
                String[] parts = message.split("\\|");
                if (parts.length >= 4) {
                    String responseData = parts[3];
                    String[] dataParts = responseData.split("\\^");
                    if (dataParts.length > 0) {
                        String stockCode = dataParts[0];
                        log.info("🔍 메시지에서 추출한 종목코드: {}", stockCode);
                        return stockCode;
                    }
                }
            } else {
                // ^ 구분자만 있는 메시지의 경우 (종목코드가 없음)
                log.warn("⚠️ 종목코드가 없는 ^ 구분자 메시지");
            }
        } catch (Exception e) {
            log.error("❌ 종목코드 추출 실패: {}", e.getMessage());
        }
        return null; // 종목코드를 찾을 수 없으면 null 반환
    }

    /**
     * ^ 구분자 형식 호가 데이터 처리
     * 형식: -50^0^0^0^258750^0^0 (변동가^매도호가1^매도수량1^매수호가1^매수수량1^...)
     */
    private void handleOrderBookData(String message, String stockCode) {
        try {
            log.info("📊 ^ 구분자 호가 데이터 파싱 시작");
            
            // 종목코드가 null이면 처리하지 않음
            if (stockCode == null) {
                log.warn("⚠️ 종목코드가 null - 호가 데이터 처리 건너뜀");
                return;
            }
            
            String[] parts = message.split("\\^");
            if (parts.length < 5) {
                log.warn("⚠️ 호가 데이터 형식이 올바르지 않음: {}", message);
                return;
            }
            
            // 기본 정보 파싱
            String changePrice = parts[0]; // 변동가
            String askPrice1 = parts[1];   // 매도호가1
            String askQuantity1 = parts[2]; // 매도수량1
            String bidPrice1 = parts[3];   // 매수호가1
            String bidQuantity1 = parts[4]; // 매수수량1
            
            log.info("📈 호가 데이터: 변동가={}, 매도1={}@{}, 매수1={}@{}", 
                    changePrice, askPrice1, askQuantity1, bidPrice1, bidQuantity1);
            
            // 종목코드는 매개변수로 전달받음
            log.info("📊 처리할 종목코드: {}", stockCode);
            
            // OrderBookResponse 생성 (간단한 형태)
            List<OrderBookItem> askPrices = new ArrayList<>();
            List<OrderBookItem> bidPrices = new ArrayList<>();
            
            if (!askPrice1.equals("0") && !askQuantity1.equals("0")) {
                askPrices.add(new OrderBookItem(
                    Float.parseFloat(askPrice1), 
                    Long.parseLong(askQuantity1), 
                    "ask"
                ));
            }
            
            if (!bidPrice1.equals("0") && !bidQuantity1.equals("0")) {
                bidPrices.add(new OrderBookItem(
                    Float.parseFloat(bidPrice1), 
                    Long.parseLong(bidQuantity1), 
                    "bid"
                ));
            }
            
            OrderBookResponse orderBook = new OrderBookResponse(
                stockCode,
                getStockName(stockCode),
                0.0f,  // currentPrice (호가 데이터에서는 현재가 정보가 없으므로 0)
                0.0f,  // changeAmount
                0.0f,  // changeRate
                "unchanged",  // changeDirection
                askPrices,
                bidPrices
            );
            
            // Redis에 캐시
            redisCacheService.cacheWebSocketOrderBook(stockCode, orderBook);
            
            // WebSocket으로 브로드캐스트
            messagingTemplate.convertAndSend("/topic/orderbook/" + stockCode, orderBook);
            
            log.info("✅ ^ 구분자 호가 데이터 처리 완료: {}", stockCode);
            
        } catch (Exception e) {
            log.error("❌ ^ 구분자 호가 데이터 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 대기 중인 지정가 주문 체결 확인
     */
    private void checkPendingLimitOrders(String stockCode) {
        try {
            // 해당 종목의 대기 중인 지정가 주문들을 조회하여 체결 가능한지 확인
            // TODO: OrderRepository에서 PENDING 상태의 지정가 주문들을 조회하는 메서드 필요
            log.debug("지정가 주문 체결 확인 - 종목코드: {}", stockCode);
        } catch (Exception e) {
            log.error("지정가 주문 체결 확인 중 오류 발생 - 종목코드: {} - {}", stockCode, e.getMessage());
        }
    }

    /**
     * 종목명 조회 (캐시 우선, DB 폴백)
     */
    private String getStockName(String stockCode) {
        // 캐시에서 먼저 조회
        if (stockNameCache.containsKey(stockCode)) {
            return stockNameCache.get(stockCode);
        }

        // DB에서 조회
        try {
            String stockName = stockRepository.findByStockCode(stockCode)
                    .map(stock -> stock.getStockName())
                    .orElse("알 수 없음");
            
            // 캐시에 저장
            stockNameCache.put(stockCode, stockName);
            return stockName;
        } catch (Exception e) {
            log.error("종목명 조회 실패 - 종목코드: {} - {}", stockCode, e.getMessage());
            return "알 수 없음";
        }
    }
}