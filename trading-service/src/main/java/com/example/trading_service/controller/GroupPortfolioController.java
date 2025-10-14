package com.example.trading_service.controller;

import com.example.trading_service.dto.ApiResponse;
import com.example.trading_service.dto.GroupPortfolioResponse;
import com.example.trading_service.dto.GroupGoalStatusResponse;
import com.example.trading_service.service.GroupPortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/test/group-portfolio")
@RequiredArgsConstructor
@Slf4j
public class GroupPortfolioController {

    private final GroupPortfolioService groupPortfolioService;

    /**
     * 그룹 포트폴리오 조회 (홈 화면용)
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupPortfolioResponse>> getGroupPortfolio(@PathVariable UUID groupId) {
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
    @GetMapping("/{groupId}/goal-status")
    public ResponseEntity<ApiResponse<GroupGoalStatusResponse>> getGroupGoalStatus(@PathVariable UUID groupId) {
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
