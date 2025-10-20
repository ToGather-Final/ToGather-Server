package com.example.user_service.controller;

import com.example.module_common.dto.InvestmentAccountDto;
import com.example.user_service.client.TradingServiceClient;
import com.example.user_service.domain.GroupMember;
import com.example.user_service.repository.GroupMemberRepository;
import com.example.user_service.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 내부 시스템 호출용 컨트롤러
 * - 다른 서비스에서 호출하는 API들을 담당
 * - 인증이 필요 없는 시스템 내부 API
 */
@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final GroupService groupService;
    private final GroupMemberRepository groupMemberRepository;
    private final TradingServiceClient tradingServiceClient;

    /**
     * 그룹 투표 정족수 조회 (내부 시스템용)
     * GET /internal/{groupId}/vote-quorum
     */
    @GetMapping("/{groupId}/vote-quorum")
    public ResponseEntity<Integer> getVoteQuorumInternal(@PathVariable UUID groupId) {
        log.info("🔍 내부 API 호출 - /internal/{}/vote-quorum", groupId);
        try {
            Integer voteQuorum = groupService.getVoteQuorumInternal(groupId);
            log.info("✅ 투표 정족수 조회 성공 - groupId: {}, voteQuorum: {}", groupId, voteQuorum);
            return ResponseEntity.ok(voteQuorum);
        } catch (Exception e) {
            log.error("❌ 투표 정족수 조회 실패 - groupId: {}, error: {}", groupId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/groups/{groupId}/members/accounts")
    public List<InvestmentAccountDto> getGroupMemberAccounts(@PathVariable UUID groupId) {
        log.info("🔍 내부 API 호출 - /internal/groups/{}/members/accounts", groupId);

        try{
            List<GroupMember> groupMembers = groupMemberRepository.findByIdGroupId(groupId);

            if(groupMembers.isEmpty()){
                log.warn("그룹에 멤버가 없습니다 - groupId: {}", groupId);
                return Collections.emptyList();
            }

            List<InvestmentAccountDto> accounts = new ArrayList<>();
            for (GroupMember gm : groupMembers) {
                try{
                    InvestmentAccountDto account = tradingServiceClient.getAccountByUserId(gm.getUserId());
                    if (account != null) {
                        accounts.add(account);
                    }
                } catch (Exception e) {
                    log.warn("멤버의 투자 계좌 조회 실패 - userId: {}, error: {}", gm.getUserId(), e.getMessage());
                }
            }
            log.info("✅ 그룹 멤버 투자 계좌 조회 완료 - groupId: {}, 멤버 수: {}, 계좌 수: {}",
                    groupId, groupMembers.size(), accounts.size());

            return accounts;
        } catch (Exception e) {
            log.error("❌ 그룹 멤버 투자 계좌 조회 실패 - groupId: {}, error: {}", groupId, e.getMessage(), e);
            throw new RuntimeException("그룹 멤버 투자 계좌 조회 실패", e);
        }
    }
}
