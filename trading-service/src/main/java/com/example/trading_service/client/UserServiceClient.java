package com.example.trading_service.client;

import com.example.module_common.dto.InvestmentAccountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

/**
 * User Service Client
 * - trading-service에서 user-service의 internal API 호출용
 */
@FeignClient(
    name = "user-service",
    url = "${app.services.user-service.url:http://localhost:8082}"
)
public interface UserServiceClient {
    
    /**
     * 그룹 멤버 목록 조회
     * - 그룹에 속한 모든 멤버의 ID 목록
     */
    @GetMapping("/internal/groups/{groupId}/members")
    List<UUID> getGroupMembers(@PathVariable UUID groupId);

    @GetMapping("/internal/groups/{groupId}/members/accounts")
    List<InvestmentAccountDto> getGroupMemberAccounts(@PathVariable("groupId") UUID groupId);

    /**
     * 사용자가 속한 그룹 목록 조회
     * - 사용자가 멤버로 속한 모든 그룹의 ID 목록
     */
    @GetMapping("/internal/users/{userId}/groups")
    List<UUID> getUserGroups(@PathVariable("userId") UUID userId);
}
