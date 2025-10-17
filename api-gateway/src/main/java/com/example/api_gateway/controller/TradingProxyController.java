package com.example.api_gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/trading")
@Slf4j
@Tag(name = "트레이딩 관리", description = "주식 거래, 포트폴리오 관리, 계좌 관리 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class TradingProxyController {

    private final WebClient tradingServiceClient;

    public TradingProxyController() {
        this.tradingServiceClient = WebClient.builder()
                .baseUrl("http://localhost:8081")
                .build();
    }

    // ==================== 계좌 관리 ====================
    
    @Operation(summary = "투자 계좌 개설", description = "사용자의 투자 계좌를 새로 개설합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "투자 계좌 개설 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/account/invest")
    public Mono<ResponseEntity<Object>> createInvestmentAccount(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        log.info("투자 계좌 개설 요청: userId={}", userId);
        
        return tradingServiceClient.post()
                .uri("/trading/account/invest")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("투자 계좌 개설 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "계좌 정보 조회", description = "사용자의 계좌 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "계좌 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/account/info")
    public Mono<ResponseEntity<Object>> getAccountInfo(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        log.info("계좌 정보 조회 요청: userId={}", userId);
        
        return tradingServiceClient.get()
                .uri("/trading/account/info")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("계좌 정보 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "계좌 정보 조회 (마스킹)", description = "마스킹된 계좌번호로 계좌 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "계좌 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/account/info/masked")
    public Mono<ResponseEntity<Object>> getAccountInfoMasked(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        log.info("계좌 정보 조회 (마스킹) 요청: userId={}", userId);
        
        return tradingServiceClient.get()
                .uri("/trading/account/info/masked")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("계좌 정보 조회 (마스킹) 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "계좌 잔고 조회", description = "사용자의 계좌 잔고를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "계좌 잔고 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/account/balance")
    public Mono<ResponseEntity<Object>> getAccountBalance(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        log.info("계좌 잔고 조회 요청: userId={}", userId);
        
        return tradingServiceClient.get()
                .uri("/trading/account/balance")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("계좌 잔고 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ==================== 주식 거래 ====================
    
    @Operation(summary = "주식 매수", description = "지정된 종목을 매수 주문합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "매수 주문 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 잔고 부족"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/trade/buy")
    public Mono<ResponseEntity<Object>> buyStock(
            @Parameter(description = "매수 요청 데이터", required = true) @RequestBody Object request,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        log.info("주식 매수 요청: userId={}", userId);
        
        return tradingServiceClient.put()
                .uri("/trading/trade/buy")
                .header("X-User-Id", userId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("주식 매수 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "주식 매도", description = "보유한 주식을 매도 주문합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "매도 주문 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 보유 수량 부족"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/trade/sell")
    public Mono<ResponseEntity<Object>> sellStock(
            @Parameter(description = "매도 요청 데이터", required = true) @RequestBody Object request,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        log.info("주식 매도 요청: userId={}", userId);
        
        return tradingServiceClient.put()
                .uri("/trading/trade/sell")
                .header("X-User-Id", userId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("주식 매도 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "예수금 충전", description = "투자 계좌에 예수금을 충전합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "예수금 충전 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/trade/deposit")
    public Mono<ResponseEntity<Object>> depositFunds(
            @Parameter(description = "예수금 충전 요청 데이터", required = true) @RequestBody Object request,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        log.info("예수금 충전 요청: userId={}", userId);
        
        return tradingServiceClient.put()
                .uri("/trading/trade/deposit")
                .header("X-User-Id", userId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("예수금 충전 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ==================== 주식 정보 ====================
    
    @Operation(summary = "주식 목록 조회", description = "거래 가능한 주식 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "주식 목록 조회 성공")
    })
    @GetMapping("/stocks")
    public Mono<ResponseEntity<Object>> getStocks(
            @Parameter(description = "검색어 (종목명 또는 종목코드)") @RequestParam(required = false) String search) {
        log.info("주식 목록 조회 요청: search={}", search);
        
        return tradingServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/trading/stocks")
                        .queryParamIfPresent("search", java.util.Optional.ofNullable(search))
                        .build())
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("주식 목록 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "주식 상세 정보 조회", description = "특정 종목의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "주식 상세 정보 조회 성공"),
        @ApiResponse(responseCode = "404", description = "종목을 찾을 수 없음")
    })
    @GetMapping("/stocks/{stockCode}")
    public Mono<ResponseEntity<Object>> getStockByCode(
            @Parameter(description = "종목코드", required = true) @PathVariable String stockCode) {
        log.info("주식 상세 정보 조회 요청: stockCode={}", stockCode);
        
        return tradingServiceClient.get()
                .uri("/trading/stocks/{stockCode}", stockCode)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("주식 상세 정보 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "주식 호가 정보 조회", description = "특정 종목의 호가 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "호가 정보 조회 성공"),
        @ApiResponse(responseCode = "404", description = "종목을 찾을 수 없음")
    })
    @GetMapping("/stocks/{stockCode}/orderbook")
    public Mono<ResponseEntity<Object>> getOrderBook(
            @Parameter(description = "종목코드", required = true) @PathVariable String stockCode) {
        log.info("주식 호가 정보 조회 요청: stockCode={}", stockCode);
        
        return tradingServiceClient.get()
                .uri("/trading/stocks/{stockCode}/orderbook", stockCode)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("주식 호가 정보 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "주식 차트 데이터 조회", description = "특정 종목의 차트 데이터를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "차트 데이터 조회 성공"),
        @ApiResponse(responseCode = "404", description = "종목을 찾을 수 없음")
    })
    @GetMapping("/stocks/{stockCode}/chart")
    public Mono<ResponseEntity<Object>> getStockChart(
            @Parameter(description = "종목코드", required = true) @PathVariable String stockCode,
            @Parameter(description = "기간 구분 (D: 일, W: 주, M: 월)") @RequestParam(defaultValue = "D") String periodDiv) {
        log.info("주식 차트 데이터 조회 요청: stockCode={}, periodDiv={}", stockCode, periodDiv);
        
        return tradingServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/trading/stocks/{stockCode}/chart")
                        .queryParam("periodDiv", periodDiv)
                        .build(stockCode))
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("주식 차트 데이터 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ==================== 포트폴리오 관리 ====================
    
    @Operation(summary = "보유 종목 조회", description = "사용자가 보유한 모든 종목의 실시간 가격 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "보유 종목 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/portfolio/holdings")
    public Mono<ResponseEntity<Object>> getHoldings(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        log.info("보유 종목 조회 요청: userId={}", userId);
        
        return tradingServiceClient.get()
                .uri("/trading/portfolio/holdings")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("보유 종목 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "포트폴리오 주식 조회", description = "사용자의 포트폴리오에 포함된 주식 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "포트폴리오 주식 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/portfolio/stocks")
    public Mono<ResponseEntity<Object>> getPortfolioStocks(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        log.info("포트폴리오 주식 조회 요청: userId={}", userId);
        
        return tradingServiceClient.get()
                .uri("/trading/portfolio/stocks")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("포트폴리오 주식 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "포트폴리오 요약 조회", description = "사용자의 포트폴리오 요약 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "포트폴리오 요약 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/portfolio/summary")
    public Mono<ResponseEntity<Object>> getPortfolioSummary(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        log.info("포트폴리오 요약 조회 요청: userId={}", userId);
        
        return tradingServiceClient.get()
                .uri("/trading/portfolio/summary")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("포트폴리오 요약 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "거래 내역 조회", description = "사용자의 거래 내역을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "거래 내역 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/portfolio/history")
    public Mono<ResponseEntity<Object>> getTradeHistory(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        log.info("거래 내역 조회 요청: userId={}", userId);
        
        return tradingServiceClient.get()
                .uri("/trading/portfolio/history")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("거래 내역 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "특정 종목 거래 내역 조회", description = "특정 종목의 거래 내역을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "특정 종목 거래 내역 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/portfolio/history/{stockCode}")
    public Mono<ResponseEntity<Object>> getStockTradeHistory(
            @Parameter(description = "종목코드", required = true) @PathVariable String stockCode,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        log.info("특정 종목 거래 내역 조회 요청: stockCode={}, userId={}", stockCode, userId);
        
        return tradingServiceClient.get()
                .uri("/trading/portfolio/history/{stockCode}", stockCode)
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("특정 종목 거래 내역 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ==================== 주문 관리 ====================
    
    @Operation(summary = "대기 중인 주문 조회", description = "사용자의 대기 중인 주문 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대기 중인 주문 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/orders/pending")
    public Mono<ResponseEntity<Object>> getPendingOrders(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        log.info("대기 중인 주문 조회 요청: userId={}", userId);
        
        return tradingServiceClient.get()
                .uri("/trading/orders/pending")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("대기 중인 주문 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "주문 취소", description = "대기 중인 주문을 취소합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "주문 취소 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    @DeleteMapping("/orders/{orderId}")
    public Mono<ResponseEntity<Object>> cancelOrder(
            @Parameter(description = "주문 ID", required = true) @PathVariable UUID orderId,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        log.info("주문 취소 요청: orderId={}, userId={}", orderId, userId);
        
        return tradingServiceClient.delete()
                .uri("/trading/orders/{orderId}", orderId)
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("주문 취소 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ==================== 그룹 포트폴리오 ====================
    
    @Operation(summary = "그룹 포트폴리오 조회", description = "특정 그룹의 투자 포트폴리오 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "그룹 포트폴리오 조회 성공"),
        @ApiResponse(responseCode = "400", description = "그룹 포트폴리오 조회 실패")
    })
    @GetMapping("/test/group-portfolio/{groupId}")
    public Mono<ResponseEntity<Object>> getGroupPortfolio(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId) {
        log.info("그룹 포트폴리오 조회 요청: groupId={}", groupId);
        
        return tradingServiceClient.get()
                .uri("/test/group-portfolio/{groupId}", groupId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("그룹 포트폴리오 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "그룹 목표 달성 상태 확인", description = "특정 그룹의 투자 목표 달성 상태를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "그룹 목표 달성 상태 확인 성공"),
        @ApiResponse(responseCode = "400", description = "그룹 목표 달성 상태 확인 실패")
    })
    @GetMapping("/test/group-portfolio/{groupId}/goal-status")
    public Mono<ResponseEntity<Object>> getGroupGoalStatus(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId) {
        log.info("그룹 목표 달성 상태 확인 요청: groupId={}", groupId);
        
        return tradingServiceClient.get()
                .uri("/test/group-portfolio/{groupId}/goal-status", groupId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("그룹 목표 달성 상태 확인 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ==================== 배치 관리 ====================
    
    @Operation(summary = "배치 처리 실행", description = "수동으로 배치 처리를 실행합니다. (관리자용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "배치 처리 실행 성공"),
        @ApiResponse(responseCode = "400", description = "배치 처리 실행 실패")
    })
    @PostMapping("/batch/execute")
    public Mono<ResponseEntity<Object>> executeBatch() {
        log.info("배치 처리 실행 요청");
        
        return tradingServiceClient.post()
                .uri("/batch/execute")
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("배치 처리 실행 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "캐시 테이블 초기화", description = "캐싱 테이블을 초기화합니다. (관리자용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "캐시 테이블 초기화 성공"),
        @ApiResponse(responseCode = "400", description = "캐시 테이블 초기화 실패")
    })
    @PostMapping("/batch/clear-cache")
    public Mono<ResponseEntity<Object>> clearCacheTables() {
        log.info("캐시 테이블 초기화 요청");
        
        return tradingServiceClient.post()
                .uri("/batch/clear-cache")
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("캐시 테이블 초기화 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "배치 처리 상태 확인", description = "배치 처리 서비스의 상태를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "배치 처리 상태 확인 성공")
    })
    @GetMapping("/batch/status")
    public Mono<ResponseEntity<Object>> getBatchStatus() {
        log.info("배치 처리 상태 확인 요청");
        
        return tradingServiceClient.get()
                .uri("/batch/status")
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("배치 처리 상태 확인 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ==================== KIS 토큰 관리 ====================
    
    @Operation(summary = "KIS 토큰 상태 조회", description = "KIS API 토큰의 상태를 조회합니다. (관리자용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "KIS 토큰 상태 조회 성공")
    })
    @GetMapping("/admin/kis/token/status")
    public Mono<ResponseEntity<Object>> getTokenStatus() {
        log.info("KIS 토큰 상태 조회 요청");
        
        return tradingServiceClient.get()
                .uri("/admin/kis/token/status")
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("KIS 토큰 상태 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "KIS 토큰 수동 갱신", description = "KIS API 토큰을 수동으로 갱신합니다. (관리자용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "KIS 토큰 갱신 성공"),
        @ApiResponse(responseCode = "400", description = "KIS 토큰 갱신 실패")
    })
    @PostMapping("/admin/kis/token/refresh")
    public Mono<ResponseEntity<Object>> refreshToken() {
        log.info("KIS 토큰 수동 갱신 요청");
        
        return tradingServiceClient.post()
                .uri("/admin/kis/token/refresh")
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("KIS 토큰 수동 갱신 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "KIS 토큰 캐시 무효화", description = "KIS API 토큰 캐시를 무효화합니다. (관리자용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "KIS 토큰 캐시 무효화 성공")
    })
    @DeleteMapping("/admin/kis/token/cache")
    public Mono<ResponseEntity<Object>> invalidateToken() {
        log.info("KIS 토큰 캐시 무효화 요청");
        
        return tradingServiceClient.delete()
                .uri("/admin/kis/token/cache")
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("KIS 토큰 캐시 무효화 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}