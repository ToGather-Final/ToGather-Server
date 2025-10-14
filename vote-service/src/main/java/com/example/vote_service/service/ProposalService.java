package com.example.vote_service.service;

import com.example.vote_service.client.UserServiceClient;
import com.example.vote_service.dto.ProposalCreateRequest;
import com.example.vote_service.model.Proposal;
import com.example.vote_service.model.ProposalStatus;
import com.example.vote_service.repository.ProposalRepository;
import com.example.vote_service.repository.GroupMembersRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Proposal 서비스
 * - 제안 생성, 조회, 상태 변경 등의 비즈니스 로직 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final GroupMembersRepository groupMembersRepository;
    private final HistoryService historyService;
    private final UserServiceClient userServiceClient;
    private final ObjectMapper objectMapper;

    /**
     * 제안 생성
     * - 사용자의 그룹을 자동으로 조회하여 제안 생성
     * TODO: GroupRule에서 voteDurationHours를 가져와 closeAt 설정
     */
    @Transactional
    public UUID createProposal(UUID userId, ProposalCreateRequest request) {
        log.info("제안 생성 시작 - userId: {}, proposalName: {}", userId, request.proposalName());
        
        // 1. 사용자의 그룹 ID 조회
        UUID groupId = getUserGroupId(userId);
        log.info("사용자 그룹 조회 완료 - userId: {}, groupId: {}", userId, groupId);
        
        // 2. 사용자 닉네임 조회
        String proposerName = userServiceClient.getUserNickname(userId);
        log.info("사용자 닉네임 조회 완료 - userId: {}, proposerName: {}", userId, proposerName);
        
        // 3. payload를 유효한 JSON으로 변환
        String validatedPayload = validateAndConvertPayload(request.payload());
        
        // 4. 투표 생성
        // 투표 종료 시간은 임시로 5분 후로 설정
        LocalDateTime closeAt = LocalDateTime.now().plusMinutes(5);
        
        Proposal proposal = Proposal.create(
                groupId,
                userId,
                request.proposalName(),
                proposerName,
                request.category(),
                request.action(),
                validatedPayload,
                closeAt
        );
        
        Proposal saved = proposalRepository.save(proposal);
        
        // 5. 히스토리 생성 (VOTE_CREATED)
        historyService.createVoteCreatedHistory(
            userId,
            saved.getProposalId(),
            request.proposalName(),
            proposerName
        );
        
        return saved.getProposalId();
    }

    /**
     * 특정 그룹의 제안 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Proposal> getProposalsByGroup(UUID groupId) {
        return proposalRepository.findByGroupId(groupId);
    }

    /**
     * 특정 그룹의 진행 중인 제안 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Proposal> getOpenProposalsByGroup(UUID groupId) {
        return proposalRepository.findByGroupIdAndStatus(groupId, ProposalStatus.OPEN);
    }

    /**
     * 제안 상세 조회
     */
    @Transactional(readOnly = true)
    public Proposal getProposal(UUID proposalId) {
        return proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("제안을 찾을 수 없습니다."));
    }

    /**
     * 제안 승인 처리
     */
    @Transactional
    public void approveProposal(UUID proposalId) {
        Proposal proposal = getProposal(proposalId);
        proposal.approve();
    }

    /**
     * 제안 거부 처리
     */
    @Transactional
    public void rejectProposal(UUID proposalId) {
        Proposal proposal = getProposal(proposalId);
        proposal.reject();
    }

    /**
     * 제안이 특정 그룹에 속하는지 검증
     */
    public void validateProposalBelongsToGroup(UUID proposalId, UUID groupId) {
        Proposal proposal = getProposal(proposalId);
        if (!proposal.getGroupId().equals(groupId)) {
            throw new IllegalArgumentException("제안이 해당 그룹에 속하지 않습니다.");
        }
    }

    /**
     * 사용자의 그룹 ID 조회
     * - ERD의 GroupMembers 테이블을 통해 사용자가 속한 그룹 조회
     * - 사용자는 하나의 그룹에만 속한다고 가정
     * 
     * @param userId 사용자 ID
     * @return 사용자가 속한 그룹 ID
     * @throws IllegalArgumentException 그룹에 속하지 않은 경우
     */
    public UUID getUserGroupId(UUID userId) {
        log.info("===== 그룹 조회 시작 =====");
        log.info("조회할 userId: {}", userId);
        log.info("userId 바이트: {}", java.util.Arrays.toString(userId.toString().getBytes()));
        
        Optional<UUID> groupIdOpt = groupMembersRepository.findFirstGroupIdByUserId(userId);
        
        if (groupIdOpt.isEmpty()) {
            log.error("❌ 그룹을 찾을 수 없습니다 - userId: {}", userId);
            log.error("데이터베이스에 해당 사용자의 group_members 데이터가 없습니다.");
            throw new IllegalArgumentException("사용자가 속한 그룹이 없습니다.");
        }
        
        UUID groupId = groupIdOpt.get();
        log.info("✅ 그룹 조회 성공 - userId: {}, groupId: {}", userId, groupId);
        log.info("===== 그룹 조회 완료 =====");
        return groupId;
    }

    /**
     * 그룹 멤버십 검증
     * - 사용자가 특정 그룹의 멤버인지 확인
     * - Spring Data JPA가 자동으로 SQL 생성: SELECT COUNT(*) > 0 FROM group_members WHERE user_id = ? AND group_id = ?
     * 
     * @param userId 사용자 ID
     * @param groupId 그룹 ID
     * @throws IllegalArgumentException 그룹 멤버가 아닌 경우
     */
    private void validateGroupMembership(UUID userId, UUID groupId) {
        if (!groupMembersRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new IllegalArgumentException("해당 그룹의 멤버가 아닙니다.");
        }
    }

    /**
     * payload를 유효한 JSON 문자열로 변환
     * - null인 경우 null 반환 (MySQL JSON 컬럼은 null 허용)
     * - Object 타입(Map, List 등)인 경우 JSON 문자열로 직렬화
     * - String 타입인 경우 유효한 JSON인지 확인 후 반환
     * 
     * @param payload 원본 payload (Object, String, null)
     * @return 유효한 JSON 문자열 또는 null
     */
    private String validateAndConvertPayload(Object payload) {
        // null인 경우 null 반환
        if (payload == null) {
            return null;
        }
        
        try {
            // String이 아닌 경우 (Map, List 등) - JSON으로 직렬화
            if (!(payload instanceof String)) {
                return objectMapper.writeValueAsString(payload);
            }
            
            // String인 경우
            String payloadStr = (String) payload;
            
            // 빈 문자열인 경우 null 반환
            if (payloadStr.trim().isEmpty()) {
                return null;
            }
            
            // 이미 유효한 JSON인지 확인 (객체 또는 배열)
            if (payloadStr.trim().startsWith("{") || payloadStr.trim().startsWith("[")) {
                // JSON 파싱을 시도하여 유효성 검증
                objectMapper.readTree(payloadStr);
                return payloadStr;
            }
            
            // 일반 문자열인 경우 JSON 객체로 래핑
            return objectMapper.writeValueAsString(
                java.util.Map.of("value", payloadStr)
            );
        } catch (JsonProcessingException e) {
            log.error("payload JSON 변환 실패: {}", payload, e);
            // 변환 실패 시 빈 JSON 객체 반환
            return "{}";
        }
    }
}

