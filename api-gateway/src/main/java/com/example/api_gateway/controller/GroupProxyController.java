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
@RequestMapping("/api/groups")
@Slf4j
@Tag(name = "그룹 관리", description = "그룹 생성, 조회, 수정, 삭제 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class GroupProxyController {

    private final WebClient userServiceClient;

    public GroupProxyController() {
        this.userServiceClient = WebClient.builder()
                .baseUrl("http://localhost:8082")
                .build();
    }

    // ==================== 그룹 조회 ====================
    
    @Operation(summary = "그룹 상세 조회", description = "특정 그룹의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "그룹 상세 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "그룹 접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @GetMapping("/{groupId}")
    public Mono<ResponseEntity<Object>> getGroup(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("그룹 상세 조회 요청: groupId={}, userId={}", groupId, userId);
        
        return userServiceClient.get()
                .uri("/groups/{groupId}", groupId)
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("그룹 상세 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "내 그룹 목록 조회", description = "현재 사용자가 속한 모든 그룹의 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "그룹 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/mine")
    public Mono<ResponseEntity<Object>> getMyGroups(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("내 그룹 목록 조회 요청: userId={}", userId);
        
        return userServiceClient.get()
                .uri("/groups/mine")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("내 그룹 목록 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "그룹 멤버 목록 조회", description = "특정 그룹의 모든 멤버 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "멤버 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "그룹 접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @GetMapping("/{groupId}/members")
    public Mono<ResponseEntity<Object>> getMembers(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("그룹 멤버 목록 조회 요청: groupId={}, userId={}", groupId, userId);
        
        return userServiceClient.get()
                .uri("/groups/{groupId}/members", groupId)
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("그룹 멤버 목록 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "그룹 상태 확인", description = "그룹의 현재 상태를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "그룹 상태 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @GetMapping("/{groupId}/status")
    public Mono<ResponseEntity<Object>> getGroupStatus(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("그룹 상태 확인 요청: groupId={}, userId={}", groupId, userId);
        
        return userServiceClient.get()
                .uri("/groups/{groupId}/status", groupId)
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("그룹 상태 확인 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "내 그룹들 상태 확인", description = "현재 사용자가 속한 모든 그룹의 상태를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "그룹 상태 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/mine/status")
    public Mono<ResponseEntity<Object>> getMyGroupStatus(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("내 그룹들 상태 확인 요청: userId={}", userId);
        
        return userServiceClient.get()
                .uri("/groups/mine/status")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("내 그룹들 상태 확인 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ==================== 그룹 생성 및 관리 ====================
    
    @Operation(summary = "그룹 생성", description = "새로운 투자 그룹을 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "그룹 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public Mono<ResponseEntity<Object>> createGroup(
            @Parameter(description = "그룹 생성 요청 데이터", required = true) @RequestBody Object request,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("그룹 생성 요청: userId={}", userId);
        
        return userServiceClient.post()
                .uri("/groups")
                .header("X-User-Id", userId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("그룹 생성 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "그룹 멤버 추가", description = "그룹에 새로운 멤버를 추가합니다. (그룹장만 가능)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "멤버 추가 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 그룹이 이미 활성화됨"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "그룹장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @PostMapping("/{groupId}/members")
    public Mono<ResponseEntity<Object>> addMember(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId,
            @Parameter(description = "멤버 추가 요청 데이터", required = true) @RequestBody Object request,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("그룹 멤버 추가 요청: groupId={}, userId={}", groupId, userId);
        
        return userServiceClient.post()
                .uri("/groups/{groupId}/members", groupId)
                .header("X-User-Id", userId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("그룹 멤버 추가 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "초대 코드 발급", description = "그룹 초대를 위한 초대 코드를 새로 발급합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "초대 코드 발급 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "그룹장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @PostMapping("/{groupId}/invites")
    public Mono<ResponseEntity<Object>> issueInvite(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("초대 코드 발급 요청: groupId={}, userId={}", groupId, userId);
        
        return userServiceClient.post()
                .uri("/groups/{groupId}/invites", groupId)
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("초대 코드 발급 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "초대 수락", description = "초대 코드를 사용하여 그룹에 참여합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "초대 수락 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 초대 코드 또는 이미 만료됨"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "초대 코드를 찾을 수 없음")
    })
    @PostMapping("/invites/{code}/accept")
    public Mono<ResponseEntity<Object>> acceptInvite(
            @Parameter(description = "초대 코드", required = true) @PathVariable String code,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("초대 수락 요청: code={}, userId={}", code, userId);
        
        return userServiceClient.post()
                .uri("/groups/invites/{code}/accept", code)
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("초대 수락 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ==================== 그룹 설정 관리 ====================
    
    @Operation(summary = "목표 금액 수정", description = "그룹의 목표 금액을 수정합니다. (그룹장만 가능)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "목표 금액 수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "그룹장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @PutMapping("/{groupId}/goal-amount")
    public Mono<ResponseEntity<Object>> updateGoalAmount(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId,
            @Parameter(description = "목표 금액 수정 요청 데이터", required = true) @RequestBody Object request,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("목표 금액 수정 요청: groupId={}, userId={}", groupId, userId);
        
        return userServiceClient.put()
                .uri("/groups/{groupId}/goal-amount", groupId)
                .header("X-User-Id", userId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("목표 금액 수정 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "투표 정족수 수정", description = "그룹의 투표 정족수와 해체 정족수를 수정합니다. (그룹장만 가능)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정족수 수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "그룹장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @PutMapping("/{groupId}/quorum")
    public Mono<ResponseEntity<Object>> updateQuorum(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId,
            @Parameter(description = "정족수 수정 요청 데이터", required = true) @RequestBody Object request,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("투표 정족수 수정 요청: groupId={}, userId={}", groupId, userId);
        
        return userServiceClient.put()
                .uri("/groups/{groupId}/quorum", groupId)
                .header("X-User-Id", userId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("투표 정족수 수정 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ==================== 시스템용 API ====================
    
    @Operation(summary = "그룹 설정 조회 (시스템용)", description = "시스템에서 사용하는 그룹 설정 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "그룹 설정 조회 성공"),
        @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @GetMapping("/internal/{groupId}/settings")
    public Mono<ResponseEntity<Object>> getGroupSettingsInternal(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId) {
        
        log.info("그룹 설정 조회 (시스템용) 요청: groupId={}", groupId);
        
        return userServiceClient.get()
                .uri("/groups/internal/{groupId}/settings", groupId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("그룹 설정 조회 (시스템용) 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}