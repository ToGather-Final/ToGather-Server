package com.example.vote_service.service;

import com.example.vote_service.dto.ProposalCreateRequest;
import com.example.vote_service.model.Proposal;
import com.example.vote_service.model.ProposalStatus;
import com.example.vote_service.repository.ProposalRepository;
import com.example.vote_service.repository.GroupMembersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Proposal 서비스
 * - 제안 생성, 조회, 상태 변경 등의 비즈니스 로직 처리
 */
@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final GroupMembersRepository groupMembersRepository;

    /**
     * 제안 생성
     * TODO: GroupRule에서 voteDurationHours를 가져와 closeAt 설정
     */
    @Transactional
    public UUID createProposal(UUID userId, ProposalCreateRequest request) {
        // 1. 그룹 멤버인지 검증
        validateGroupMembership(userId, request.groupId());
        
        // 2. 투표 생성
        // 투표 종료 시간은 임시로 5분 후로 설정
        LocalDateTime closeAt = LocalDateTime.now().plusMinutes(5);
        
        Proposal proposal = Proposal.create(
                request.groupId(),
                userId,
                request.proposalName(),
                request.category(),
                request.action(),
                request.payload(),
                closeAt
        );
        
        Proposal saved = proposalRepository.save(proposal);
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
}

