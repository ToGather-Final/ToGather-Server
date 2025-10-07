package com.example.vote_service.scheduler;

import com.example.vote_service.model.Proposal;
import com.example.vote_service.model.ProposalStatus;
import com.example.vote_service.repository.ProposalRepository;
import com.example.vote_service.service.VoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 투표 자동 집계 스케줄러
 * - 주기적으로 마감 시간이 지난 제안들을 집계
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoteScheduler {

    private final ProposalRepository proposalRepository;
    private final VoteService voteService;

    /**
     * 매 분마다 실행되어 마감된 투표를 자동 집계
     * TODO: user-service에서 GroupRule과 멤버 수를 가져와서 정확한 집계 수행
     */
    @Scheduled(cron = "0 * * * * *") // 매 분 0초에 실행
    public void tallyExpiredVotes() {
        log.info("투표 자동 집계 스케줄러 시작");

        // 진행 중(OPEN) 상태의 모든 제안 조회
        List<Proposal> openProposals = proposalRepository.findByStatus(ProposalStatus.OPEN);

        for (Proposal proposal : openProposals) {
            // 마감 시간이 지난 제안만 집계
            if (proposal.isExpired()) {
                try {
                    log.info("제안 집계 시작: proposalId={}, proposalName={}", 
                            proposal.getProposalId(), proposal.getProposalName());

                    // TODO: user-service API 호출하여 GroupRule과 멤버 수 가져오기
                    // GroupRule rule = userServiceClient.getGroupRule(proposal.getGroupId());
                    // int totalMembers = userServiceClient.getGroupMemberCount(proposal.getGroupId());
                    // voteService.tallyVotes(proposal.getProposalId(), totalMembers, rule.getVoteQuorum());

                    // 임시: 간단 버전 사용
                    voteService.tallyVotesSimple(proposal.getProposalId());

                    log.info("제안 집계 완료: proposalId={}", proposal.getProposalId());
                } catch (Exception e) {
                    log.error("제안 집계 실패: proposalId={}, error={}", 
                            proposal.getProposalId(), e.getMessage(), e);
                }
            }
        }

        log.info("투표 자동 집계 스케줄러 종료");
    }
}

