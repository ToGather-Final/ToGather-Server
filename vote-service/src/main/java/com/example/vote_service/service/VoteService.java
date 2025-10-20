package com.example.vote_service.service;

import com.example.vote_service.client.UserServiceClient;
import com.example.vote_service.client.TradingServiceClient;
import com.example.vote_service.dto.VoteRequest;
import com.example.vote_service.dto.InternalDepositRequest;
import com.example.vote_service.dto.payload.TradePayload;
import com.example.vote_service.dto.payload.PayPayload;
import com.example.vote_service.event.VoteExpirationEvent;
import com.example.vote_service.model.ProposalStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final TradingServiceClient tradingServiceClient;
    private final ObjectMapper objectMapper;

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

        checkAndExecuteIfQuorumReached(proposalId, proposal.getGroupId());
        
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

        // 가결 조건: 찬성 투표 수가 정족수(voteQuorum) 이상
        boolean isApproved = (approveCount >= voteQuorum);
        
        log.info("가결 여부: {} (찬성 >= 정족수: {})", 
                isApproved, (approveCount >= voteQuorum));

        if (isApproved) {
            proposalService.approveProposal(proposalId);
            
            // 히스토리 생성 (VOTE_APPROVED) - 실제 payload에서 정보 읽어오기 (먼저 생성)
            createVoteApprovedHistoryFromProposal(proposal);
            
            // PAY 카테고리 투표 가결 시 자동 예수금 충전 (투표 가결 히스토리 생성 이후 처리)
            if (proposal.getCategory().name().equals("PAY")) {
                processPayVoteApproval(proposal);
            }
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
     * PAY 투표 가결 시 자동 예수금 충전 처리
     * - 그룹의 모든 멤버들에게 인당 충전 금액을 투자계좌에 자동 충전
     */
    private void processPayVoteApproval(Proposal proposal) {
        try {
            // PayPayload에서 충전 금액 정보 추출
            PayPayload payPayload = objectMapper.readValue(proposal.getPayload(), PayPayload.class);
            BigDecimal amountPerPerson = BigDecimal.valueOf(payPayload.amountPerPerson());
            
            // 그룹의 모든 멤버 조회
            List<UUID> memberIds = groupMembersRepository.findUserIdsByGroupId(proposal.getGroupId());
            
            log.info("PAY 투표 가결 - 그룹: {}, 인당 충전 금액: {}, 멤버 수: {}", 
                    proposal.getGroupId(), amountPerPerson, memberIds.size());
            
            // 각 멤버에게 예수금 충전
            for (UUID memberId : memberIds) {
                try {
                    InternalDepositRequest depositRequest = new InternalDepositRequest(
                            memberId,
                            amountPerPerson,
                            proposal.getGroupId(),
                            "투표 가결에 따른 예수금 충전 - " + proposal.getProposalName()
                    );
                    
                    tradingServiceClient.internalDepositFunds(depositRequest);
                    
                    log.info("예수금 충전 완료 - 사용자: {}, 금액: {}", memberId, amountPerPerson);
                    
                } catch (Exception e) {
                    log.error("예수금 충전 실패 - 사용자: {}, 금액: {}, 오류: {}", 
                            memberId, amountPerPerson, e.getMessage(), e);
                    // 개별 사용자 충전 실패는 전체 프로세스를 중단하지 않음
                }
            }
            
            // 예수금 충전 완료 히스토리 생성 (그룹 단위로 하나만)
            historyService.createCashDepositCompletedHistory(
                    proposal.getGroupId(),
                    proposal.getProposalName(),
                    amountPerPerson.intValue(),
                    memberIds.size(),
                    memberIds
            );
            
            log.info("PAY 투표 가결 처리 완료 - 그룹: {}, 총 처리 멤버: {}", 
                    proposal.getGroupId(), memberIds.size());
                    
        } catch (Exception e) {
            log.error("PAY 투표 가결 처리 중 오류 발생 - proposalId: {}, 오류: {}", 
                    proposal.getProposalId(), e.getMessage(), e);
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
            Integer voteQuorum = userServiceClient.getVoteQuorumInternal(groupId);
            
            log.info("그룹 정족수 조회 완료 - groupId: {}, 정족수: {}", 
                    groupId, voteQuorum);

            tallyVotes(proposalId, 0, voteQuorum); // totalMembers는 사용하지 않으므로 0으로 설정
            
            log.info("투표 마감 집계 완료 - proposalId: {}", proposalId);
        } catch (Exception e) {
            log.error("❌ user-service API 호출 실패로 투표 마감 집계 실패 - proposalId: {}, groupId: {}, error: {}", 
                    proposalId, groupId, e.getMessage(), e);
            
            // API 호출 실패 시 투표 집계를 중단 (기본값 사용하지 않음)
            throw new RuntimeException("투표 정족수 조회 실패로 인한 집계 중단", e);
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
    
    /**
     * Proposal에서 실제 정보를 읽어와서 VOTE_APPROVED 히스토리 생성
     */
    private void createVoteApprovedHistoryFromProposal(Proposal proposal) {
        try {
            if (proposal.getCategory().name().equals("TRADE")) {
                // TRADE 카테고리: payload에서 실제 매매 정보 읽어오기
                TradePayload tradePayload = objectMapper.readValue(proposal.getPayload(), TradePayload.class);
                
                historyService.createVoteApprovedHistory(
                    proposal.getGroupId(),
                    proposal.getProposalId(),
                    LocalDateTime.now().toString(), // scheduledAt - 함수가 실행된 시점
                    "TRADE", // historyType - TRADE
                    proposal.getAction().name(), // side - BUY/SELL
                    tradePayload.stockName(), // stockName
                    tradePayload.quantity(), // shares
                    tradePayload.price(), // unitPrice
                    "KRW", // currency
                    tradePayload.stockId() // stockId - UUID 객체로 전달 (BINARY(16) 변환 자동 처리)
                );
                
                log.info("✅ TRADE 투표 가결 히스토리 생성 - stockName: {}, quantity: {}, price: {}", 
                        tradePayload.stockName(), tradePayload.quantity(), tradePayload.price());
                        
            } else if (proposal.getCategory().name().equals("PAY")) {
                // PAY 카테고리: 예수금 충전 안내 메시지
                PayPayload payPayload = objectMapper.readValue(proposal.getPayload(), PayPayload.class);
                
                historyService.createVoteApprovedHistory(
                    proposal.getGroupId(),
                    proposal.getProposalId(),
                    LocalDateTime.now().toString(), // scheduledAt - 함수가 실행된 시점
                    "PAY", // historyType - PAY
                    "PAY", // side - PAY로 고정
                    null, // stockName - PAY에서는 null
                    null, // shares - PAY에서는 null
                    payPayload.amountPerPerson(), // unitPrice - 1인당 금액
                    null, // currency - PAY에서는 null
                    null // stockId - PAY에서는 null
                );
                
                log.info("✅ PAY 투표 가결 히스토리 생성 - amountPerPerson: {}, message: 예수금 충전이 자동으로 진행됩니다.", 
                        payPayload.amountPerPerson());
                        
            } else {
                // 다른 카테고리: 히스토리 생성하지 않음
                log.info("기타 카테고리 투표 가결 - 히스토리 생성 생략: proposalId: {}, category: {}", 
                        proposal.getProposalId(), proposal.getCategory());
            }
        } catch (Exception e) {
            log.error("❌ VOTE_APPROVED 히스토리 생성 실패 - proposalId: {}, error: {}", 
                    proposal.getProposalId(), e.getMessage(), e);
            // 실패 시 히스토리 생성하지 않음 (에러 로그만 남김)
        }
    }

    private void checkAndExecuteIfQuorumReached(UUID proposalId, UUID groupId) {
        try {
            long approveCount = countApproveVotes(proposalId);
            long rejectCount = countRejectVotes(proposalId);

            Integer voteQuorum = userServiceClient.getVoteQuorumInternal(groupId);

            log.info("정족수 확인 - proposalId: {}, 찬성: {}, 반대: {}, 정족수: {}",
                    proposalId, approveCount, rejectCount, voteQuorum);

            boolean isApproved = (approveCount >= voteQuorum) && (approveCount > rejectCount);

            if (isApproved) {
                log.info("🎉 정족수 도달! 즉시 투표 집계 실행 - proposalId: {}", proposalId);

                tallyVotesImmediately(proposalId, voteQuorum);

                Proposal proposal = proposalService.getProposal(proposalId);
                if (proposal.getStatus() == ProposalStatus.APPROVED) {
                    log.info("🚀 즉시 거래 실행 시작 - proposalId: {}", proposalId);
                    proposalService.executeVoteBasedTrading(proposalId);
                }
            }
        } catch (Exception e) {
            log.error("정족수 확인 중 오류 발생 - proposalId: {}, error: {}",
                    proposalId, e.getMessage(), e);
        }
    }

    private void tallyVotesImmediately(UUID proposalId, Integer voteQuorum) {
        Proposal proposal = proposalService.getProposal(proposalId);

        if (!proposal.isOpen()) {
            throw new IllegalStateException("이미 종료된 제안입니다.");
        }

        long approveCount = countApproveVotes(proposalId);
        long rejectCount = countRejectVotes(proposalId);

        log.info("즉시 투표 집계 결과 - proposalId: {}, 찬성: {}, 반대: {}, 정족수: {}",
                proposalId, approveCount, rejectCount, voteQuorum);

        // 가결 조건 확인
        boolean isApproved = (approveCount >= voteQuorum) && (approveCount > rejectCount);

        log.info("가결 여부: {} (찬성 >= 정족수: {}, 찬성 > 반대: {})",
                isApproved, (approveCount >= voteQuorum), (approveCount > rejectCount));

        if (isApproved) {
            proposalService.approveProposal(proposalId);
            log.info("투표 가결 확인 - 거래 실행 시작: proposalId={}", proposalId);
        } else {
            proposalService.rejectProposal(proposalId);
            log.info("투표 부결: proposalId={}", proposalId);
        }
    }

    /**
     * 여러 제안의 찬성 투표 수를 한 번에 조회 (성능 최적화)
     */
    @Transactional(readOnly = true)
    public Map<UUID, Long> getApproveVoteCounts(List<UUID> proposalIds) {
        if (proposalIds.isEmpty()) {
            return Map.of();
        }
        
        List<Object[]> results = voteRepository.countApprovesByProposalIds(proposalIds);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],  // proposalId
                        row -> (Long) row[1]   // count
                ));
    }

    /**
     * 여러 제안의 반대 투표 수를 한 번에 조회 (성능 최적화)
     */
    @Transactional(readOnly = true)
    public Map<UUID, Long> getRejectVoteCounts(List<UUID> proposalIds) {
        if (proposalIds.isEmpty()) {
            return Map.of();
        }
        
        List<Object[]> results = voteRepository.countRejectsByProposalIds(proposalIds);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],  // proposalId
                        row -> (Long) row[1]   // count
                ));
    }

    /**
     * 사용자의 여러 제안에 대한 투표 선택을 한 번에 조회 (성능 최적화)
     */
    @Transactional(readOnly = true)
    public Map<UUID, VoteChoice> getUserVoteChoices(UUID userId, List<UUID> proposalIds) {
        if (proposalIds.isEmpty()) {
            return Map.of();
        }
        
        List<Vote> votes = voteRepository.findByUserIdAndProposalIdIn(userId, proposalIds);
        return votes.stream()
                .collect(Collectors.toMap(
                        Vote::getProposalId,
                        Vote::getChoice
                ));
    }
}

