package com.example.vote_service.client;

import com.example.vote_service.config.FeignConfig;
import com.example.vote_service.dto.GroupMemberCountResponse;
import com.example.vote_service.dto.GroupRuleResponse;
import com.example.vote_service.dto.UserMeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * User Service API 클라이언트
 * - Feign을 사용하여 user-service와 통신
 */
@FeignClient(name = "user-service", url = "http://localhost:8082", configuration = FeignConfig.class)
public interface UserServiceClient {

    /**
     * 그룹 규칙 조회
     * GET /groups/{groupId}/rules
     */
    @GetMapping("/groups/{groupId}/rules")
    GroupRuleResponse getGroupRule(@PathVariable UUID groupId);

    /**
     * 그룹 멤버 수 조회
     * GET /groups/{groupId}/members/count
     */
    @GetMapping("/groups/{groupId}/members/count")
    GroupMemberCountResponse getGroupMemberCount(@PathVariable UUID groupId);

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
}