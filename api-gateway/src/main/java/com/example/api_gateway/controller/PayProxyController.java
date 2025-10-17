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
@RequestMapping("/api/pay")
@Slf4j
@Tag(name = "결제 관리", description = "결제, 이체, 계좌 관리 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class PayProxyController {

    private final WebClient payServiceClient;

    public PayProxyController() {
        this.payServiceClient = WebClient.builder()
                .baseUrl("http://localhost:8083")
                .build();
    }

    // ==================== 계좌 관리 ====================
    
    @Operation(summary = "사용자 계좌 목록 조회", description = "현재 사용자의 모든 계좌 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "계좌 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/accounts")
    public Mono<ResponseEntity<Object>> getUserAccounts(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("사용자 계좌 목록 조회 요청: userId={}", userId);
        
        return payServiceClient.get()
                .uri("/pay/accounts")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("계좌 목록 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "계좌 상세 조회", description = "특정 계좌의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "계좌 상세 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "계좌 접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음")
    })
    @GetMapping("/accounts/{accountId}")
    public Mono<ResponseEntity<Object>> getAccount(
            @Parameter(description = "계좌 ID", required = true) @PathVariable UUID accountId,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("계좌 상세 조회 요청: accountId={}, userId={}", accountId, userId);
        
        return payServiceClient.get()
                .uri("/pay/accounts/{accountId}", accountId)
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("계좌 상세 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "그룹 페이 계좌 조회", description = "특정 그룹의 페이 계좌 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "그룹 페이 계좌 조회 성공"),
        @ApiResponse(responseCode = "404", description = "그룹 페이 계좌를 찾을 수 없음")
    })
    @GetMapping("/accounts/group/{groupId}")
    public Mono<ResponseEntity<Object>> getGroupPayAccount(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId) {
        
        log.info("그룹 페이 계좌 조회 요청: groupId={}", groupId);
        
        return payServiceClient.get()
                .uri("/pay/accounts/group/{groupId}", groupId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("그룹 페이 계좌 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "그룹 페이 계좌 생성", description = "특정 그룹을 위한 페이 계좌를 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "그룹 페이 계좌 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "409", description = "이미 그룹 페이 계좌가 존재함")
    })
    @PostMapping("/accounts/group-pay/{groupId}")
    public Mono<ResponseEntity<Object>> createGroupPayAccount(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId,
            @Parameter(description = "그룹 페이 계좌 생성 요청 데이터", required = true) @RequestBody Object request,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("그룹 페이 계좌 생성 요청: groupId={}, userId={}", groupId, userId);
        
        return payServiceClient.post()
                .uri("/pay/accounts/group-pay/{groupId}", groupId)
                .header("X-User-Id", userId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("그룹 페이 계좌 생성 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "그룹 페이 계좌 존재 확인", description = "특정 그룹에 페이 계좌가 존재하는지 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "그룹 페이 계좌 존재 여부 확인 성공")
    })
    @GetMapping("/accounts/group-pay/exists/{groupId}")
    public Mono<ResponseEntity<Object>> hasGroupPayAccount(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId) {
        
        log.info("그룹 페이 계좌 존재 확인 요청: groupId={}", groupId);
        
        return payServiceClient.get()
                .uri("/pay/accounts/group-pay/exists/{groupId}", groupId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("그룹 페이 계좌 존재 확인 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ==================== 결제 관리 ====================
    
    @Operation(summary = "결제 처리", description = "새로운 결제를 처리합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "결제 처리 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "계좌 접근 권한 없음")
    })
    @PostMapping("/payments")
    public Mono<ResponseEntity<Object>> createPayment(
            @Parameter(description = "결제 요청 데이터", required = true) @RequestBody Object request,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("결제 처리 요청: userId={}", userId);
        
        return payServiceClient.post()
                .uri("/payments")
                .header("X-User-Id", userId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("결제 처리 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "결제 내역 조회", description = "특정 결제 ID로 결제 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "결제 내역 조회 성공"),
        @ApiResponse(responseCode = "404", description = "결제 내역을 찾을 수 없음")
    })
    @GetMapping("/payments/{paymentId}")
    public Mono<ResponseEntity<Object>> getPayment(
            @Parameter(description = "결제 ID", required = true) @PathVariable UUID paymentId) {
        
        log.info("결제 내역 조회 요청: paymentId={}", paymentId);
        
        return payServiceClient.get()
                .uri("/payments/{paymentId}", paymentId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("결제 내역 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "결제 내역 목록 조회", description = "사용자의 결제 내역을 페이지네이션하여 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "결제 내역 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "계좌 접근 권한 없음")
    })
    @GetMapping("/payments/history")
    public Mono<ResponseEntity<Object>> getPaymentHistory(
            @Parameter(description = "계좌 ID", required = true) @RequestParam UUID accountId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (1-100)", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("결제 내역 목록 조회 요청: accountId={}, page={}, size={}, userId={}", accountId, page, size, userId);
        
        return payServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/payments/history")
                        .queryParam("accountId", accountId)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("결제 내역 목록 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ==================== 이체 관리 ====================
    
    @Operation(summary = "페이머니 충전", description = "그룹 페이머니를 충전합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "페이머니 충전 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/transfers/recharge")
    public Mono<ResponseEntity<Object>> rechargePayMoney(
            @Parameter(description = "충전 요청 데이터", required = true) @RequestBody Object request,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "그룹 ID", required = true) @RequestHeader("X-Group-Id") UUID groupId) {
        
        log.info("페이머니 충전 요청: userId={}, groupId={}", userId, groupId);
        
        return payServiceClient.post()
                .uri("/transfers/recharge")
                .header("X-User-Id", userId)
                .header("X-Group-Id", groupId.toString())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("페이머니 충전 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "이체 내역 조회", description = "특정 이체 ID로 이체 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "이체 내역 조회 성공"),
        @ApiResponse(responseCode = "404", description = "이체 내역을 찾을 수 없음")
    })
    @GetMapping("/transfers/{transferId}")
    public Mono<ResponseEntity<Object>> getTransfer(
            @Parameter(description = "이체 ID", required = true) @PathVariable UUID transferId) {
        
        log.info("이체 내역 조회 요청: transferId={}", transferId);
        
        return payServiceClient.get()
                .uri("/transfers/{transferId}", transferId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("이체 내역 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "이체 내역 목록 조회", description = "사용자의 이체 내역을 페이지네이션하여 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "이체 내역 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/transfers/history")
    public Mono<ResponseEntity<Object>> getTransferHistory(
            @Parameter(description = "계좌 ID", required = true) @RequestParam UUID accountId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("이체 내역 목록 조회 요청: accountId={}, page={}, size={}, userId={}", accountId, page, size, userId);
        
        return payServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfers/history")
                        .queryParam("accountId", accountId)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("이체 내역 목록 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}