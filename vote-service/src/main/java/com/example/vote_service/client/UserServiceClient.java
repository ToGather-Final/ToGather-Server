package com.example.vote_service.client;

import com.example.module_common.dto.InvestmentAccountDto;
import com.example.vote_service.config.FeignConfig;
import com.example.vote_service.dto.GroupRuleResponse;
import com.example.vote_service.dto.UserMeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

/**
 * User Service API 클라이언트
 * - Feign을 사용하여 user-service와 통신
 */
@FeignClient(name = "user-service", url = "${app.services.user-service.url:http://localhost:8082}", configuration = FeignConfig.class)
public interface UserServiceClient {


    /**
     * 시스템용 그룹 투표 정족수 조회
     * GET /internal/{groupId}/vote-quorum
     * - 인증 없이 조회 (시스템 내부용)
     */
    @GetMapping("/internal/{groupId}/vote-quorum")
    Integer getVoteQuorumInternal(@PathVariable UUID groupId);

    /**
     * 사용자 닉네임 조회
     * GET /users/{userId}/nickname
     */
    @GetMapping("/users/{userId}/nickname")
    String getUserNickname(@PathVariable UUID userId);

    /**
     * 현재 인증된 사용자 정보 조회
     * GET /users/me
     * - X-User-Id 헤더를 통해 사용자 ID 전달
     */
    @GetMapping("/users/me")
    UserMeResponse getCurrentUser();

    /**
     * 그룹 멤버들의 투자 계좌 정보 조회
     * GET /internal/groups/{groupId}/members/accounts
     */
    @GetMapping("/internal/groups/{groupId}/members/accounts")
    List<InvestmentAccountDto> getGroupMemberAccounts(@PathVariable UUID groupId);

    /**
     * 시스템용 그룹 멤버 수 조회
     * GET /internal/{groupId}/member-count
     * - 인증 없이 조회 (시스템 내부용)
     */
    @GetMapping("/internal/{groupId}/member-count")
    Integer getGroupMemberCountInternal(@PathVariable UUID groupId);
}