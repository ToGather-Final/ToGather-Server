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
@RequestMapping("/api/vote")
@Slf4j
@Tag(name = "투표 관리", description = "투표 제안, 참여, 집계 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class VoteProxyController {

    private final WebClient voteServiceClient;

    public VoteProxyController() {
        this.voteServiceClient = WebClient.builder()
                .baseUrl("http://localhost:8084")
                .build();
    }

    // ==================== 투표 조회 ====================
    
    @Operation(summary = "투표 목록 조회", description = "사용자 그룹의 투표 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "투표 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public Mono<ResponseEntity<Object>> getProposals(
            @Parameter(description = "카테고리 필터 (TRADE, PAY 등)") @RequestParam(required = false) String view,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("투표 목록 조회 요청: userId={}, view={}", userId, view);
        
        return voteServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/vote")
                        .queryParamIfPresent("view", java.util.Optional.ofNullable(view))
                        .build())
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("투표 목록 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ==================== 투표 제안 및 참여 ====================
    
    @Operation(summary = "투표 제안 생성", description = "새로운 투표 제안을 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "투표 제안 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public Mono<ResponseEntity<Object>> createProposal(
            @Parameter(description = "투표 제안 생성 요청 데이터", required = true) @RequestBody Object request,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("투표 제안 생성 요청: userId={}", userId);
        
        return voteServiceClient.post()
                .uri("/vote")
                .header("X-User-Id", userId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("투표 제안 생성 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "투표 참여", description = "특정 투표에 참여합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "투표 참여 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/{proposalId}")
    public Mono<ResponseEntity<Object>> vote(
            @Parameter(description = "투표 ID", required = true) @PathVariable UUID proposalId,
            @Parameter(description = "투표 요청 데이터", required = true) @RequestBody Object request,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("투표 참여 요청: proposalId={}, userId={}", proposalId, userId);
        
        return voteServiceClient.post()
                .uri("/vote/{proposalId}", proposalId)
                .header("X-User-Id", userId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("투표 참여 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ==================== 투표 집계 ====================
    
    @Operation(summary = "투표 집계 및 종료", description = "투표를 집계하고 종료합니다. (관리자용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "투표 집계 성공"),
        @ApiResponse(responseCode = "400", description = "투표 집계 실패")
    })
    @PostMapping("/{proposalId}/tally")
    public Mono<ResponseEntity<Object>> tallyVotes(
            @Parameter(description = "투표 ID", required = true) @PathVariable UUID proposalId,
            @Parameter(description = "그룹 전체 멤버 수") @RequestParam(required = false) Integer totalMembers,
            @Parameter(description = "투표 정족수") @RequestParam(required = false) Integer voteQuorum) {
        
        log.info("투표 집계 요청: proposalId={}, totalMembers={}, voteQuorum={}", proposalId, totalMembers, voteQuorum);
        
        return voteServiceClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/vote/{proposalId}/tally")
                        .queryParamIfPresent("totalMembers", java.util.Optional.ofNullable(totalMembers))
                        .queryParamIfPresent("voteQuorum", java.util.Optional.ofNullable(voteQuorum))
                        .build(proposalId))
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("투표 집계 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}