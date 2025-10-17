package com.example.trading_service.service;

import com.example.trading_service.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class KisWebSocketClient {

    private final KisTokenService kisTokenService;
    private final WebSocketOrderBookService webSocketOrderBookService;
    private final StockRepository stockRepository;

    private WebSocket webSocket;
    private boolean isConnected = false;
    private boolean hasReceivedResponse = false;
    private String approvalKey;

    private final List<String> subscribedStocks = new CopyOnWriteArrayList<>();

    /**
     * 한투 WebSocket에 연결
     */
    public void connect() {
        try {
            log.info("🔗 connect() 메서드 시작");
            // 1. approval_key 발급
            approvalKey = kisTokenService.getWebSocketApprovalKey();
            log.info("approval_key 발급 완료");

            // 2. WebSocket URL 구성 (실전투자 도메인 사용)
            String wsUrl = "ws://ops.koreainvestment.com:21000"; // 실전투자 도메인
            log.info("WebSocket 연결 시도 (실전투자): {}", wsUrl);

            // 3. WebSocket 연결
            HttpClient client = HttpClient.newHttpClient();
            CompletableFuture<WebSocket> webSocketFuture = client.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        log.info("✅ 한투 WebSocket 연결 성공!");
                        log.info("🔗 WebSocket 인스턴스 설정 완료");
                        isConnected = true;
                        
                        // webSocket 인스턴스 설정
                        KisWebSocketClient.this.webSocket = webSocket;
                        
                        // 수신 루프 시작
                        webSocket.request(1);
                        log.info("🔄 WebSocket 수신 루프 시작");
                        
                        // 연결 성공 후 잠시 대기 후 구독 시작
                        CompletableFuture.runAsync(() -> {
                            try {
                                log.info("⏳ 1초 대기 후 주식 구독 시작...");
                                Thread.sleep(1000); // 1초 대기
                                log.info("🚀 주식 구독 시작!");
                                subscribeAllStocks();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                log.error("구독 대기 중 스레드 인터럽트 발생", e);
                            }
                        });
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        log.info("📩 한투 WebSocket 메시지 수신: {}", data);
                        hasReceivedResponse = true; // 응답 수신 플래그 설정
                        handleMessage(data.toString());
                        // 수신 루프 계속 요청
                        webSocket.request(1);
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.error("❌ WebSocket 에러 발생", error);
                        isConnected = false;
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        log.warn("WebSocket 연결 종료: {} - {}", statusCode, reason);
                        isConnected = false;
                        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                    }
                });

            // WebSocket 연결 완료 대기 (이미 onOpen에서 webSocket 인스턴스 설정됨)
            webSocketFuture.get();
            log.info("WebSocket 클라이언트 초기화 완료");

        } catch (Exception e) {
            log.error("WebSocket 연결 실패", e);
            isConnected = false;
        }
    }

    /**
     * 20개 주식/ETF 전체 구독
     */
    private void subscribeAllStocks() {
        // DB에서 활성화된 종목들 조회
        List<String> stockCodes = stockRepository.findByEnabledTrue()
                .stream()
                .map(stock -> stock.getStockCode())
                .collect(java.util.stream.Collectors.toList());
        
        log.info("📊 {}개 주식/ETF 전체 구독 시작", stockCodes.size());
        log.info("📈 구독 대상: {}", stockCodes);

        try {
            for (String stockCode : stockCodes) {
                log.info("📈 주식 {} 구독 중...", stockCode);
                subscribeOrderBook(stockCode);
                Thread.sleep(100); // 100ms 간격으로 구독 (API 제한 고려)
            }
            
            log.info("✅ {}개 주식/ETF 구독 완료", stockCodes.size());
            subscribedStocks.addAll(stockCodes);
            
        } catch (Exception e) {
            log.error("❌ 주식 구독 실패", e);
        }
    }

    /**
     * 개별 주식 호가 구독
     */
    private void subscribeOrderBook(String stockCode) {
        try {
            if (webSocket == null || !isConnected) {
                log.warn("⚠️ WebSocket이 연결되지 않음 - 구독 실패: {}", stockCode);
                return;
            }

            // 한투 API 호가 구독 메시지 형식
            String subscribeMessage = String.format(
                "{\"header\":{\"approval_key\":\"%s\",\"custtype\":\"P\",\"tr_type\":\"1\",\"content-type\":\"utf-8\"},\"body\":{\"input\":{\"tr_id\":\"H0STASP0\",\"tr_key\":\"%s\"}}}",
                approvalKey, stockCode
            );

            log.info("📤 호가 구독 메시지 전송: {} -> {}", stockCode, subscribeMessage);
            webSocket.sendText(subscribeMessage, true);
            
            // 구독 성공 시 리스트에 추가
            if (!subscribedStocks.contains(stockCode)) {
                subscribedStocks.add(stockCode);
            }
            
        } catch (Exception e) {
            log.error("❌ 호가 구독 실패 - 종목코드: {}", stockCode, e);
        }
    }

    /**
     * WebSocket 메시지 처리
     */
    private void handleMessage(String message) {
        try {
            // WebSocketOrderBookService에 메시지 전달
            webSocketOrderBookService.handleOrderBookMessage(message);
        } catch (Exception e) {
            log.error("❌ WebSocket 메시지 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * WebSocket 연결 상태 확인
     */
    public boolean isConnected() {
        return isConnected && webSocket != null;
    }

    /**
     * 구독된 주식 목록 조회
     */
    public List<String> getSubscribedStocks() {
        return new CopyOnWriteArrayList<>(subscribedStocks);
    }

    /**
     * WebSocket 연결 해제
     */
    public void disconnect() {
        try {
            if (webSocket != null) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "정상 종료");
                webSocket = null;
            }
            isConnected = false;
            subscribedStocks.clear();
            log.info("🔌 WebSocket 연결 해제 완료");
        } catch (Exception e) {
            log.error("WebSocket 연결 해제 실패", e);
        }
    }

    /**
     * WebSocket 재연결
     */
    public void reconnect() {
        log.info("🔄 WebSocket 재연결 시도");
        disconnect();
        try {
            Thread.sleep(2000); // 2초 대기
            connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("재연결 대기 중 스레드 인터럽트 발생", e);
        }
    }

    /**
     * 특정 주식 구독 해제
     */
    public void unsubscribeStock(String stockCode) {
        try {
            if (webSocket == null || !isConnected) {
                log.warn("⚠️ WebSocket이 연결되지 않음 - 구독 해제 실패: {}", stockCode);
                return;
            }

            // 한투 API 호가 구독 해제 메시지 형식
            String unsubscribeMessage = String.format(
                "{\"header\":{\"approval_key\":\"%s\",\"custtype\":\"P\",\"tr_type\":\"2\",\"content-type\":\"utf-8\"},\"body\":{\"input\":{\"tr_id\":\"H0STASP0\",\"tr_key\":\"%s\"}}}",
                approvalKey, stockCode
            );

            log.info("📤 호가 구독 해제 메시지 전송: {} -> {}", stockCode, unsubscribeMessage);
            webSocket.sendText(unsubscribeMessage, true);
            
            // 구독 해제 시 리스트에서 제거
            subscribedStocks.remove(stockCode);
            
        } catch (Exception e) {
            log.error("❌ 호가 구독 해제 실패 - 종목코드: {}", stockCode, e);
        }
    }

    /**
     * 모든 주식 구독 해제
     */
    public void unsubscribeAllStocks() {
        log.info("📊 모든 주식 구독 해제 시작");
        
        try {
            for (String stockCode : new CopyOnWriteArrayList<>(subscribedStocks)) {
                unsubscribeStock(stockCode);
                Thread.sleep(100); // 100ms 간격으로 구독 해제
            }
            
            log.info("✅ 모든 주식 구독 해제 완료");
            
        } catch (Exception e) {
            log.error("❌ 모든 주식 구독 해제 실패", e);
        }
    }

    /**
     * WebSocket 상태 정보 조회
     */
    public String getStatus() {
        return String.format(
            "WebSocket 상태 - 연결: %s, 구독 종목: %d개, 응답 수신: %s",
            isConnected ? "연결됨" : "연결 안됨",
            subscribedStocks.size(),
            hasReceivedResponse ? "수신됨" : "수신 안됨"
        );
    }
}