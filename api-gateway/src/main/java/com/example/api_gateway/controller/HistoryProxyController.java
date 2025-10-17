package com.example.api_gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/history")
@Slf4j
@Tag(name = "히스토리 관리", description = "사용자 활동 히스토리 조회 관련 API")
public class HistoryProxyController {

    private final WebClient voteServiceClient;

    public HistoryProxyController() {
        this.voteServiceClient = WebClient.builder()
                .baseUrl("http://localhost:8084")
                .build();
    }

    // ==================== 히스토리 조회 ====================
    
    @Operation(summary = "사용자 히스토리 조회", description = "사용자의 활동 히스토리를 페이지네이션하여 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "히스토리 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public Mono<ResponseEntity<Object>> getHistory(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size) {
        
        log.info("사용자 히스토리 조회 요청: userId={}, page={}, size={}", userId, page, size);
        
        return voteServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/history")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("사용자 히스토리 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "사용자 히스토리 전체 조회", description = "사용자의 모든 활동 히스토리를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "히스토리 전체 조회 성공"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/all")
    public Mono<ResponseEntity<Object>> getAllHistory(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("사용자 히스토리 전체 조회 요청: userId={}", userId);
        
        return voteServiceClient.get()
                .uri("/history/all")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("사용자 히스토리 전체 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}