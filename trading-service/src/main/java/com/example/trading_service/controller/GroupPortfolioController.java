package com.example.trading_service.controller;

import com.example.trading_service.dto.ApiResponse;
import com.example.trading_service.dto.GroupPortfolioResponse;
import com.example.trading_service.dto.GroupGoalStatusResponse;
import com.example.trading_service.service.GroupPortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/test/group-portfolio")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "그룹 포트폴리오", description = "그룹 투자 포트폴리오 및 목표 달성 상태 관련 API")
public class GroupPortfolioController {

    private final GroupPortfolioService groupPortfolioService;

    /**
     * 그룹 포트폴리오 조회 (홈 화면용)
     */
    @Operation(summary = "그룹 포트폴리오 조회", description = "특정 그룹의 투자 포트폴리오 정보를 조회합니다. (홈 화면용)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "그룹 포트폴리오 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "그룹 포트폴리오 조회 실패")
    })
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupPortfolioResponse>> getGroupPortfolio(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId) {
        log.info("그룹 포트폴리오 조회 요청 - 그룹 ID: {}", groupId);

        try {
            GroupPortfolioResponse portfolio = groupPortfolioService.getGroupPortfolio(groupId);
            return ResponseEntity.ok(ApiResponse.success(portfolio));
        } catch (Exception e) {
            log.error("그룹 포트폴리오 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ApiResponse.error("그룹 포트폴리오 조회 실패: " + e.getMessage(), "GROUP_PORTFOLIO_ERROR")
            );
        }
    }

    /**
     * 그룹 목표 달성 상태 확인
     */
    @Operation(summary = "그룹 목표 달성 상태 확인", description = "특정 그룹의 투자 목표 달성 상태를 확인합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "그룹 목표 달성 상태 확인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "그룹 목표 달성 상태 확인 실패")
    })
    @GetMapping("/{groupId}/goal-status")
    public ResponseEntity<ApiResponse<GroupGoalStatusResponse>> getGroupGoalStatus(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId) {
        log.info("그룹 목표 달성 상태 확인 요청 - 그룹 ID: {}", groupId);
        
        try {
            GroupGoalStatusResponse goalStatus = groupPortfolioService.getGroupGoalStatus(groupId);
            return ResponseEntity.ok(ApiResponse.success(goalStatus));
        } catch (Exception e) {
            log.error("그룹 목표 달성 상태 확인 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ApiResponse.error("그룹 목표 달성 상태 확인 실패: " + e.getMessage(), "GROUP_GOAL_STATUS_ERROR")
            );
        }
    }
}
