package com.example.vote_service.service;

import com.example.vote_service.client.UserServiceClient;
import com.example.vote_service.dto.GroupRuleResponse;
import com.example.vote_service.dto.VoteRequest;
import com.example.vote_service.event.VoteExpirationEvent;
import com.example.vote_service.model.Proposal;
import com.example.vote_service.model.Vote;
import com.example.vote_service.model.VoteChoice;
import com.example.vote_service.repository.VoteRepository;
import com.example.vote_service.repository.GroupMembersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Vote 서비스
 * - 투표 생성, 조회, 집계 등의 비즈니스 로직 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final ProposalService proposalService;
    private final GroupMembersRepository groupMembersRepository;
    private final HistoryService historyService;
    private final UserServiceClient userServiceClient;

    /**
     * 투표하기
     * - 이미 투표한 경우 투표 변경
     * - 처음 투표하는 경우 새로 생성
     * - 투표만 저장하고, 가결/부결은 마감 시간에 집계 시 결정됨
     */
    @Transactional
    public UUID vote(UUID userId, UUID proposalId, VoteRequest request) {
        // 제안이 투표 가능한 상태인지 확인 (진행중 + 마감 전)
        Proposal proposal = proposalService.getProposal(proposalId);
        
        // 그룹 멤버인지 검증
        validateGroupMembership(userId, proposal.getGroupId());
        if (!proposal.canVote()) {
            if (proposal.isExpired()) {
                throw new IllegalStateException("투표 마감 시간이 지났습니다.");
            }
            throw new IllegalStateException("종료된 제안에는 투표할 수 없습니다.");
        }

        // 이미 투표했는지 확인
        Optional<Vote> existingVote = voteRepository.findByProposalIdAndUserId(proposalId, userId);
        
        Vote savedVote;
        if (existingVote.isPresent()) {
            // 투표 변경
            Vote vote = existingVote.get();
            vote.changeChoice(request.choice());
            savedVote = vote;
        } else {
            // 새 투표 생성
            Vote vote = Vote.create(proposalId, userId, request.choice());
            savedVote = voteRepository.save(vote);
        }

        // 투표 완료
        // 마감 시간(closeAt)에 스케줄러가 자동으로 가결/부결 판단
        log.info("투표 완료 - proposalId: {}, userId: {}", proposalId, userId);
        
        return savedVote.getVoteId();
    }

    /**
     * 특정 제안의 모든 투표 조회
     */
    @Transactional(readOnly = true)
    public List<Vote> getVotesByProposal(UUID proposalId) {
        return voteRepository.findByProposalId(proposalId);
    }

    /**
     * 특정 제안의 찬성 투표 수
     */
    @Transactional(readOnly = true)
    public long countApproveVotes(UUID proposalId) {
        return voteRepository.countByProposalIdAndChoice(proposalId, VoteChoice.AGREE);
    }

    /**
     * 특정 제안의 반대 투표 수
     */
    @Transactional(readOnly = true)
    public long countRejectVotes(UUID proposalId) {
        return voteRepository.countByProposalIdAndChoice(proposalId, VoteChoice.DISAGREE);
    }

    /**
     * 특정 제안의 총 투표 수
     */
    @Transactional(readOnly = true)
    public long countTotalVotes(UUID proposalId) {
        return voteRepository.countByProposalId(proposalId);
    }

    /**
     * 사용자가 특정 제안에 투표했는지 확인
     */
    @Transactional(readOnly = true)
    public boolean hasVoted(UUID proposalId, UUID userId) {
        return voteRepository.existsByProposalIdAndUserId(proposalId, userId);
    }

    /**
     * 사용자의 투표 선택 조회 (AGREE, DISAGREE, NEUTRAL)
     */
    @Transactional(readOnly = true)
    public VoteChoice getUserVoteChoice(UUID proposalId, UUID userId) {
        Optional<Vote> vote = voteRepository.findByProposalIdAndUserId(proposalId, userId);
        return vote.map(Vote::getChoice).orElse(VoteChoice.NEUTRAL);
    }

    /**
     * 투표 결과 집계 및 제안 상태 업데이트
     * - 투표 마감 시간에 호출됨 (closeAt 기준)
     * - GroupRule의 voteQuorum(정족수)을 사용하여 가결/부결 결정
     * 
     * @param proposalId 제안 ID
     * @param totalMembers 그룹 전체 멤버 수
     * @param voteQuorum 투표 정족수 (정수, 예: 3 = 3명 이상 찬성 필요)
     */
    @Transactional
    public void tallyVotes(UUID proposalId, int totalMembers, int voteQuorum) {
        Proposal proposal = proposalService.getProposal(proposalId);
        
        // 이미 종료된 제안인지 확인
        if (!proposal.isOpen()) {
            throw new IllegalStateException("이미 종료된 제안입니다.");
        }

        // 투표 마감 시간 확인
        if (!proposal.isExpired()) {
            throw new IllegalStateException("아직 투표 마감 시간이 되지 않았습니다.");
        }

        // 투표 집계
        long approveCount = countApproveVotes(proposalId);
        long rejectCount = countRejectVotes(proposalId);
        long totalVotes = approveCount + rejectCount;

        // 투표 결과 로깅
        log.info("투표 집계 결과 - proposalId: {}, 찬성: {}, 반대: {}, 정족수: {}", 
                proposalId, approveCount, rejectCount, voteQuorum);

        // 가결 조건:
        // 1. 찬성 투표 수가 정족수(voteQuorum) 이상
        // 2. 찬성이 반대보다 많음 (동점이면 부결)
        boolean isApproved = (approveCount >= voteQuorum) && (approveCount > rejectCount);
        
        log.info("가결 여부: {} (찬성 >= 정족수: {}, 찬성 > 반대: {})", 
                isApproved, (approveCount >= voteQuorum), (approveCount > rejectCount));

        if (isApproved) {
            proposalService.approveProposal(proposalId);
            // 히스토리 생성 (VOTE_APPROVED)
            // TODO: 실제 매매 정보를 payload에 포함해야 함
            historyService.createVoteApprovedHistory(
                proposal.getGroupId(),
                proposalId,
                "2024-01-01 15:00:00", // scheduledAt - TODO: 실제 실행 예정 시간
                "BUY", // side - TODO: 실제 매매 방향
                "삼성전자", // stockName - TODO: 실제 주식명
                100, // shares - TODO: 실제 주식 수량
                70000, // unitPrice - TODO: 실제 주가
                "KRW" // currency - TODO: 실제 통화
            );
        } else {
            proposalService.rejectProposal(proposalId);
            // 히스토리 생성 (VOTE_REJECTED)
            historyService.createVoteRejectedHistory(
                proposal.getGroupId(),
                proposalId,
                proposal.getProposalName()
            );
        }
    }


    /**
     * 투표 결과 집계 (간단 버전 - 정족수 정보 없이)
     * TODO: user-service에서 GroupRule과 멤버 수를 가져와서 위의 메서드 호출
     */
    @Transactional
    public void tallyVotesSimple(UUID proposalId) {
        Proposal proposal = proposalService.getProposal(proposalId);
        
        if (!proposal.isOpen()) {
            throw new IllegalStateException("이미 종료된 제안입니다.");
        }

        if (!proposal.isExpired()) {
            throw new IllegalStateException("아직 투표 마감 시간이 되지 않았습니다.");
        }

        long approveCount = countApproveVotes(proposalId);
        long rejectCount = countRejectVotes(proposalId);

        // 임시: 찬성이 반대보다 많으면 승인
        // TODO: 실제로는 위의 tallyVotes(proposalId, totalMembers, voteQuorum) 사용
        if (approveCount > rejectCount) {
            proposalService.approveProposal(proposalId);
        } else {
            proposalService.rejectProposal(proposalId);
        }
    }

    /**
     * 투표 마감 이벤트 리스너
     * - ProposalService에서 발행하는 VoteExpirationEvent를 처리
     * - 순환 참조 없이 투표 마감 시 집계 수행
     */
    @EventListener
    @Transactional
    public void handleVoteExpiration(VoteExpirationEvent event) {
        UUID proposalId = event.proposalId();
        UUID groupId = event.groupId();
        
        log.info("투표 마감 이벤트 수신 - proposalId: {}, groupId: {}", proposalId, groupId);
        
        try {
            // user-service에서 정족수 정보 가져오기 (시스템용 API 사용)
            GroupRuleResponse rule = userServiceClient.getVoteQuorumInternal(groupId);
            
            log.info("그룹 정족수 조회 완료 - groupId: {}, 정족수: {}", 
                    groupId, rule.voteQuorum());

            tallyVotes(proposalId, 0, rule.voteQuorum()); // totalMembers는 사용하지 않으므로 0으로 설정
            
            log.info("투표 마감 집계 완료 - proposalId: {}", proposalId);
        } catch (Exception e) {
            log.error("투표 마감 집계 실패 - proposalId: {}, error: {}", proposalId, e.getMessage(), e);
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

