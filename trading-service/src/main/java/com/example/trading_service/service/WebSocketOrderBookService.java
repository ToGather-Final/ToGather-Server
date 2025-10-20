package com.example.trading_service.service;

import com.example.trading_service.domain.Order;
import com.example.trading_service.domain.Stock;
import com.example.trading_service.dto.OrderBookItem;
import com.example.trading_service.dto.OrderBookResponse;
import com.example.trading_service.dto.StockPriceResponse;
import com.example.trading_service.repository.OrderRepository;
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
    private final OrderRepository orderRepository;

    // 종목명 캐시 (성능 최적화를 위해)
    private final Map<String, String> stockNameCache = new HashMap<>();

    /**
     * 한투 WebSocket 메시지를 파싱하고 브로드캐스트
     */
    public void handleOrderBookMessage(String message) {
        try {
            log.debug("📨 호가 메시지 수신");
            
            // 실시간 데이터인지 확인 (|로 구분되는 형식)
            if (message.contains("|")) {
                log.debug("실시간 파이프 구분 데이터 수신");
                handleRealtimeData(message);
                return;
            }
            
            // ^ 구분자 형식 메시지 처리 (호가 데이터)
            if (message.contains("^")) {
                log.debug("실시간 ^ 구분자 데이터 수신");
                // | 구분자로 시작하는 메시지에서 종목코드 추출
                String stockCode = extractStockCodeFromMessage(message);
                handleOrderBookData(message, stockCode);
                return;
            }
            
            // JSON 형식 메시지 처리
            JsonNode rootNode = objectMapper.readTree(message);
            
            // AppKey 중복 사용 오류 처리
            if (isAppKeyInUseError(rootNode)) {
                log.error("❌ AppKey 중복 사용 오류 감지 - 구독 중단");
                // AppKey 중복 사용 시 구독을 즉시 중단
                return;
            }
            
            // 구독 한도 초과 오류 처리
            if (isMaxSubscribeOverError(rootNode)) {
                log.warn("⚠️ 구독 한도 초과 - 더 이상 구독할 수 없습니다");
                return;
            }
            
            // PINGPONG 메시지 처리
            if (isPingPongMessage(rootNode)) {
                log.info("💓 PINGPONG 하트비트 메시지 수신 - 연결 유지 중");
                return;
            }
            
            // 구독 성공 메시지 처리
            if (isSubscribeSuccessMessage(rootNode)) {
                log.info("✅ 주식 구독 성공 메시지 수신");
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
                log.debug("호가 데이터 파싱 성공: {}", orderBook.getStockCode());
                
                // 1. Redis 캐시에 저장 (30초 TTL)
                redisCacheService.cacheWebSocketOrderBook(orderBook.getStockCode(), orderBook);
                
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
     * 구독 성공 메시지인지 확인
     */
    private boolean isSubscribeSuccessMessage(JsonNode rootNode) {
        try {
            JsonNode body = rootNode.get("body");
            if (body != null) {
                String msg1 = body.get("msg1").asText();
                return "SUBSCRIBE SUCCESS".equals(msg1);
            }
        } catch (Exception e) {
            // 구독 성공 메시지가 아닌 경우 무시
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
     * AppKey 중복 사용 오류인지 확인
     */
    private boolean isAppKeyInUseError(JsonNode rootNode) {
        try {
            JsonNode body = rootNode.get("body");
            if (body != null) {
                String msgCd = body.get("msg_cd").asText();
                String msg1 = body.get("msg1").asText();
                return "OPSP8996".equals(msgCd) && msg1.contains("ALREADY IN USE appkey");
            }
        } catch (Exception e) {
            // AppKey 오류가 아닌 경우 무시
        }
        return false;
    }
    
    /**
     * 구독 한도 초과 오류인지 확인
     */
    private boolean isMaxSubscribeOverError(JsonNode rootNode) {
        try {
            JsonNode body = rootNode.get("body");
            if (body != null) {
                String msgCd = body.get("msg_cd").asText();
                String msg1 = body.get("msg1").asText();
                return "OPSP0008".equals(msgCd) && "MAX SUBSCRIBE OVER".equals(msg1);
            }
        } catch (Exception e) {
            // 구독 한도 오류가 아닌 경우 무시
        }
        return false;
    }
    
    /**
     * AppKey 중복 사용 오류 처리
     */
    private void handleAppKeyInUseError() {
        try {
            log.warn("🔄 AppKey 중복 사용으로 인한 웹소켓 재연결 시도...");
            
            // KisWebSocketClient에 재연결 요청
            // 이 부분은 KisWebSocketClient를 주입받아서 처리하거나
            // 이벤트를 발생시켜서 처리할 수 있습니다.
            
            // 현재는 로그만 남기고, 실제 재연결은 모니터링 스케줄러에서 처리
            log.warn("⚠️ AppKey 중복 사용 오류 - 다음 모니터링 주기에서 재연결 시도 예정");
            
        } catch (Exception e) {
            log.error("❌ AppKey 중복 사용 오류 처리 중 예외 발생", e);
        }
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
                } else if ("0".equals(encryptionFlag) && "H0STCNT0".equals(trId)) {
                    // 암호화되지 않은 현재가 데이터 처리
                    parseRealtimeCurrentPrice(responseData);
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
            
            if (dataParts.length < 5) {
                log.warn("⚠️ 호가 데이터 필드가 부족함: {}", dataParts.length);
                return;
            }
            
            // 첫 번째 필드에서 종목코드 추출 (예: 005930^091217^0^97400^97500...)
            String stockCode = dataParts[0];
            
            // 호가 데이터 파싱 (간단한 형태)
            List<OrderBookItem> askPrices = new ArrayList<>();
            List<OrderBookItem> bidPrices = new ArrayList<>();
            
            // 한투 API 호가 데이터 형식에 따른 파싱
            // 형식: 종목코드^시간^현재가^매도호가1^매도호가2^...^매도호가10^매수호가1^매수호가2^...^매수호가10^매도수량1^매도수량2^...^매도수량10^매수수량1^매수수량2^...^매수수량10
            
            // 매도 호가 (ASK) - 10개 (인덱스 3~12)
            int askCount = 0;
            for (int i = 3; i < 13 && i < dataParts.length; i++) {
                String priceStr = dataParts[i];
                
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
                                        log.debug("매도 수량 파싱 실패: {}", quantityStr);
                                    }
                                }
                            }
                            // 수량이 0보다 큰 경우만 추가 (0주는 제외)
                            if (quantity > 0) {
                                askPrices.add(new OrderBookItem(price, quantity, "ask"));
                                askCount++;
                            }
                        }
                    } catch (NumberFormatException e) {
                        log.debug("매도 호가 파싱 실패: {} - {}", priceStr, e.getMessage());
                    }
                }
            }
            
            // 매수 호가 (BID) - 10개 (인덱스 13~22)
            int bidCount = 0;
            for (int i = 13; i < 23 && i < dataParts.length; i++) {
                String priceStr = dataParts[i];
                
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
                                        log.debug("매수 수량 파싱 실패: {}", quantityStr);
                                    }
                                }
                            }
                            // 수량이 0보다 큰 경우만 추가 (0주는 제외)
                            if (quantity > 0) {
                                bidPrices.add(new OrderBookItem(price, quantity, "bid"));
                                bidCount++;
                            }
                        }
                    } catch (NumberFormatException e) {
                        log.debug("매수 호가 파싱 실패: {} - {}", priceStr, e.getMessage());
                    }
                }
            }
            
            // 호가 데이터가 비어있으면 이전 캐시 데이터 사용 (불완전한 메시지 방지)
            if (askPrices.isEmpty() || bidPrices.isEmpty()) {
                Object cachedData = redisCacheService.getCachedWebSocketOrderBook(stockCode);
                if (cachedData instanceof OrderBookResponse) {
                    OrderBookResponse cached = (OrderBookResponse) cachedData;
                    if (askPrices.isEmpty() && !cached.getAskPrices().isEmpty()) {
                        askPrices = new ArrayList<>(cached.getAskPrices());
                        log.debug("이전 캐시 매도 호가 사용: {}건", askPrices.size());
                    }
                    if (bidPrices.isEmpty() && !cached.getBidPrices().isEmpty()) {
                        bidPrices = new ArrayList<>(cached.getBidPrices());
                        log.debug("이전 캐시 매수 호가 사용: {}건", bidPrices.size());
                    }
                }
            }
            
            // 요약 로그만 출력
            log.info("📊 호가 파싱 완료 - 종목: {}, 매도: {}건, 매수: {}건", stockCode, askCount, bidCount);
            
            // 현재가는 호가 데이터(H0STASP0)에 포함되지 않으므로
            // 매도1호가와 매수1호가의 중간값으로 실시간 추정
            float currentPrice = 0.0f;
            
            if (!askPrices.isEmpty() && !bidPrices.isEmpty()) {
                // 매도1호가(최저 매도가)와 매수1호가(최고 매수가)의 중간값
                float askPrice1 = askPrices.get(0).getPrice();
                float bidPrice1 = bidPrices.get(0).getPrice();
                currentPrice = (askPrice1 + bidPrice1) / 2.0f;
                
                log.debug("💰 현재가 추정: {}원 (매도1: {}, 매수1: {})", 
                    currentPrice, askPrice1, bidPrice1);
            } else {
                // 호가 데이터가 없으면 이전 캐시 값 사용 (fallback)
                Object cachedData = redisCacheService.getCachedWebSocketOrderBook(stockCode);
                if (cachedData instanceof OrderBookResponse) {
                    OrderBookResponse cached = (OrderBookResponse) cachedData;
                    if (cached.getCurrentPrice() > 0) {
                        currentPrice = cached.getCurrentPrice();
                        log.debug("이전 캐시 현재가 사용: {}원", currentPrice);
                    }
                }
            }
            
            // 전일 대비 정보 계산 (전일 종가와 현재가 비교)
            float changeAmount = 0.0f;
            float changeRate = 0.0f;
            String changeDirection = "unchanged";
            
            try {
                // Redis에서 전일 종가 조회 (캐시됨)
                Float prevClosePrice = getPrevClosePrice(stockCode);
                
                log.info("🔍 [{}] 전일대비 계산 - 현재가: {}, 전일종가: {}", 
                    stockCode, currentPrice, prevClosePrice);
                
                if (prevClosePrice != null && prevClosePrice > 0 && currentPrice > 0) {
                    changeAmount = currentPrice - prevClosePrice;
                    changeRate = (changeAmount / prevClosePrice) * 100.0f;
                    
                    // 변동 방향 결정
                    if (changeAmount > 0) {
                        changeDirection = "rise";
                    } else if (changeAmount < 0) {
                        changeDirection = "fall";
                    } else {
                        changeDirection = "unchanged";
                    }
                    
                    log.info("✅ [{}] 전일대비 계산 완료 - 변동: {}원({}%), 방향: {}", 
                        stockCode, changeAmount, String.format("%.2f", changeRate), changeDirection);
                } else {
                    log.warn("⚠️ [{}] 전일대비 계산 실패 - 전일종가 없음 (prevClose: {}, currentPrice: {})", 
                        stockCode, prevClosePrice, currentPrice);
                }
            } catch (Exception e) {
                log.error("❌ [{}] 전일대비 계산 실패: {}", stockCode, e.getMessage(), e);
            }
            
            // OrderBookResponse 생성
            OrderBookResponse orderBook = new OrderBookResponse(
                stockCode,
                getStockName(stockCode),
                currentPrice,
                changeAmount,
                changeRate,
                changeDirection,
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
     * 실시간 현재가 데이터 파싱 (H0STCNT0)
     * 형식: 종목코드^현재가^전일대비^등락률^거래량^거래대금^시가^고가^저가^...
     */
    private void parseRealtimeCurrentPrice(String responseData) {
        try {
            // ^로 구분되는 데이터 파싱
            String[] dataParts = responseData.split("\\^");
            log.info("📈 실시간 현재가 데이터: {}개 필드", dataParts.length);
            
            if (dataParts.length < 10) {
                log.warn("⚠️ 현재가 데이터 필드가 부족함: {}", dataParts.length);
                return;
            }
            
            // 현재가 데이터 파싱
            String stockCode = dataParts[0];
            String currentPriceStr = dataParts[1];
            String changeAmountStr = dataParts[2];
            String changeRateStr = dataParts[3];
            String volumeStr = dataParts[4];
            String openPriceStr = dataParts[6];
            String highPriceStr = dataParts[7];
            String lowPriceStr = dataParts[8];
            
            log.info("📊 현재가 데이터 - 종목: {}, 현재가: {}, 변동: {}, 변동률: {}%", 
                    stockCode, currentPriceStr, changeAmountStr, changeRateStr);
            
            // StockPriceResponse 생성
            StockPriceResponse priceResponse = new StockPriceResponse();
            priceResponse.setStockCode(stockCode);
            priceResponse.setCurrentPrice(new java.math.BigDecimal(currentPriceStr));
            priceResponse.setChangePrice(new java.math.BigDecimal(changeAmountStr));
            priceResponse.setChangeRate(Float.parseFloat(changeRateStr));
            priceResponse.setVolume(Long.parseLong(volumeStr));
            priceResponse.setOpenPrice(new java.math.BigDecimal(openPriceStr));
            priceResponse.setHighPrice(new java.math.BigDecimal(highPriceStr));
            priceResponse.setLowPrice(new java.math.BigDecimal(lowPriceStr));
            priceResponse.setPrevClosePrice(priceResponse.getCurrentPrice().subtract(priceResponse.getChangePrice()));
            
            // Redis에 주식 가격 캐시 (StockPriceService에서 사용하는 키 형식)
            cacheStockPriceFromWebSocket(stockCode, priceResponse);
            
            // WebSocket으로 브로드캐스트
            messagingTemplate.convertAndSend("/topic/stockprice/" + stockCode, priceResponse);
            
            log.info("✅ 실시간 현재가 데이터 처리 완료: {} - 현재가: {}", stockCode, currentPriceStr);
            
        } catch (Exception e) {
            log.error("❌ 실시간 현재가 데이터 파싱 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 웹소켓에서 받은 주식 가격 데이터를 Redis에 캐시
     */
    private void cacheStockPriceFromWebSocket(String stockCode, StockPriceResponse priceResponse) {
        try {
            // Stock 엔티티에서 UUID 조회
            Stock stock = stockRepository.findByStockCode(stockCode).orElse(null);
            if (stock == null) {
                log.warn("⚠️ 주식 정보를 찾을 수 없음: {}", stockCode);
                return;
            }
            
            // Redis에 캐시 (StockPriceService와 동일한 키 형식 사용)
            redisCacheService.cacheStockPrice(stock.getId(), priceResponse);
            log.info("💾 웹소켓 주식 가격 캐시 저장: {} - 현재가: {}", stockCode, priceResponse.getCurrentPrice());
            
        } catch (Exception e) {
            log.error("❌ 웹소켓 주식 가격 캐시 실패: {}", stockCode, e);
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
            log.debug("호가 데이터 브로드캐스트 완료: {}", destination);
        } catch (Exception e) {
            log.error("❌ 호가 데이터 브로드캐스트 실패: {}", e.getMessage(), e);
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
     * WebSocket 호가 업데이트마다 자동 호출
     */
    private void checkPendingLimitOrders(String stockCode) {
        try {
            // 해당 종목의 대기 중인 주문들 조회
            List<Order> pendingOrders = orderRepository.findByStock_StockCodeAndStatus(stockCode, Order.Status.PENDING);
            
            if (pendingOrders.isEmpty()) {
                return;
            }
            
            log.info("🔍 대기 중인 지정가 주문 {}건 체결 확인 - 종목: {}", pendingOrders.size(), stockCode);
            
            // 각 주문에 대해 체결 가능 여부 확인
            for (Order order : pendingOrders) {
                try {
                    tradeExecutionService.checkLimitOrderExecution(order);
                } catch (Exception e) {
                    log.error("주문 체결 확인 실패 - 주문ID: {} - {}", order.getOrderId(), e.getMessage());
                }
            }
            
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
    
    /**
     * 전일 종가 조회 (Redis 캐시 우선)
     */
    private Float getPrevClosePrice(String stockCode) {
        return redisCacheService.getCachedPrevClosePrice(stockCode);
    }
}