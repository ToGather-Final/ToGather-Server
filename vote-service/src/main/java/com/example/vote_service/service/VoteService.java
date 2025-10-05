package com.example.vote_service.service;

import com.example.vote_service.dto.VoteRequest;
import com.example.vote_service.model.Proposal;
import com.example.vote_service.model.Vote;
import com.example.vote_service.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Vote 서비스
 * - 투표 생성, 조회, 집계 등의 비즈니스 로직 처리
 */
@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final ProposalService proposalService;

    /**
     * 투표하기
     * - 이미 투표한 경우 투표 변경
     * - 처음 투표하는 경우 새로 생성
     */
    @Transactional
    public UUID vote(UUID userId, UUID proposalId, VoteRequest request) {
        // TODO: 그룹 멤버인지 검증 필요
        
        // 제안이 진행 중인지 확인
        Proposal proposal = proposalService.getProposal(proposalId);
        if (!proposal.isOpen()) {
            throw new IllegalStateException("종료된 제안에는 투표할 수 없습니다.");
        }

        // 이미 투표했는지 확인
        Optional<Vote> existingVote = voteRepository.findByProposalIdAndUserId(proposalId, userId);
        
        if (existingVote.isPresent()) {
            // 투표 변경
            Vote vote = existingVote.get();
            vote.changeChoice(request.choice());
            return vote.getVoteId();
        } else {
            // 새 투표 생성
            Vote vote = Vote.create(proposalId, userId, request.choice());
            Vote saved = voteRepository.save(vote);
            return saved.getVoteId();
        }
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
        return voteRepository.countByProposalIdAndChoice(proposalId, true);
    }

    /**
     * 특정 제안의 반대 투표 수
     */
    @Transactional(readOnly = true)
    public long countRejectVotes(UUID proposalId) {
        return voteRepository.countByProposalIdAndChoice(proposalId, false);
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
     * 투표 결과 집계 및 제안 상태 업데이트
     * TODO: GroupRule의 voteQuorum을 사용하여 정족수 체크
     */
    @Transactional
    public void tallyVotes(UUID proposalId) {
        Proposal proposal = proposalService.getProposal(proposalId);
        
        if (!proposal.isOpen()) {
            throw new IllegalStateException("이미 종료된 제안입니다.");
        }

        long approveCount = countApproveVotes(proposalId);
        long rejectCount = countRejectVotes(proposalId);

        // 임시: 찬성이 반대보다 많으면 승인
        // TODO: 실제로는 GroupRule의 voteQuorum과 비교해야 함
        if (approveCount > rejectCount) {
            proposalService.approveProposal(proposalId);
        } else {
            proposalService.rejectProposal(proposalId);
        }
    }
}

