package com.example.vote_service.controller;

import com.example.vote_service.dto.*;
import com.example.vote_service.model.Proposal;
import com.example.vote_service.service.ProposalService;
import com.example.vote_service.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Vote Controller
 * - 투표 관련 API 엔드포인트
 */
@RestController
@RequestMapping("/vote")
@RequiredArgsConstructor
public class VoteController {

    private final ProposalService proposalService;
    private final VoteService voteService;

    /**
     * GET /vote - 투표 목록 조회 (전체/예매/페이)
     * view 파라미터로 필터링 (view=TRADE, view=PAY 등)
     */
    @GetMapping
    public ResponseEntity<List<ProposalSummaryResponse>> getProposals(
            @RequestParam(required = false) String view,
            @RequestParam UUID groupId,
            Authentication authentication) {
        
        UUID userId = (UUID) authentication.getPrincipal();
        
        List<Proposal> proposals = proposalService.getProposalsByGroup(groupId);
        
        // view 파라미터로 필터링
        if (view != null && !view.isBlank()) {
            proposals = proposals.stream()
                    .filter(p -> p.getCategory().name().equalsIgnoreCase(view))
                    .collect(Collectors.toList());
        }
        
        List<ProposalSummaryResponse> response = proposals.stream()
                .map(p -> new ProposalSummaryResponse(
                        p.getProposalId(),
                        p.getProposalName(),
                        p.getCategory(),
                        p.getStatus(),
                        p.getOpenAt(),
                        p.getCloseAt()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /vote - 투표 제안
     * (예수, 마도, 예수금 총칭, 페이 머니 설정)
     */
    @PostMapping
    public ResponseEntity<UUID> createProposal(
            @Valid @RequestBody ProposalCreateRequest request,
            Authentication authentication) {
        
        UUID userId = (UUID) authentication.getPrincipal();
        UUID proposalId = proposalService.createProposal(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(proposalId);
    }

    /**
     * POST /vote/{proposalId} - 각각의 개인이 투표 (찬성/반대)
     */
    @PostMapping("/{proposalId}")
    public ResponseEntity<Void> vote(
            @PathVariable UUID proposalId,
            @Valid @RequestBody VoteRequest request,
            Authentication authentication) {
        
        UUID userId = (UUID) authentication.getPrincipal();
        voteService.vote(userId, proposalId, request);
        
        return ResponseEntity.ok().build();
    }

    /**
     * GET /vote/{proposalId} - 특정 제안 상세 조회
     */
    @GetMapping("/{proposalId}")
    public ResponseEntity<ProposalResponse> getProposal(
            @PathVariable UUID proposalId,
            Authentication authentication) {
        
        UUID userId = (UUID) authentication.getPrincipal();
        Proposal proposal = proposalService.getProposal(proposalId);
        
        long approveCount = voteService.countApproveVotes(proposalId);
        long rejectCount = voteService.countRejectVotes(proposalId);
        boolean hasVoted = voteService.hasVoted(proposalId, userId);
        
        ProposalResponse response = new ProposalResponse(
                proposal.getProposalId(),
                proposal.getGroupId(),
                proposal.getUserId(),
                proposal.getProposalName(),
                proposal.getCategory(),
                proposal.getAction(),
                proposal.getPayload(),
                proposal.getStatus(),
                proposal.getOpenAt(),
                proposal.getCloseAt(),
                approveCount,
                rejectCount,
                hasVoted
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /vote/{proposalId}/result - 투표 결과 조회
     */
    @GetMapping("/{proposalId}/result")
    public ResponseEntity<VoteResultResponse> getVoteResult(@PathVariable UUID proposalId) {
        
        long approveCount = voteService.countApproveVotes(proposalId);
        long rejectCount = voteService.countRejectVotes(proposalId);
        long totalVotes = voteService.countTotalVotes(proposalId);
        
        // 임시: 찬성이 반대보다 많으면 통과
        boolean isPassed = approveCount > rejectCount;
        
        VoteResultResponse response = new VoteResultResponse(
                proposalId,
                totalVotes,
                approveCount,
                rejectCount,
                isPassed
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /vote/{proposalId}/tally - 투표 집계 및 종료
     * (관리자 또는 자동 스케줄러가 호출)
     * 
     * Query Parameters:
     * - totalMembers: 그룹 전체 멤버 수 (필수)
     * - voteQuorum: 투표 정족수 0.0~1.0 (필수, 예: 0.5 = 50%)
     * 
     * TODO: user-service에서 GroupRule과 멤버 수를 자동으로 조회하도록 개선
     */
    @PostMapping("/{proposalId}/tally")
    public ResponseEntity<Void> tallyVotes(
            @PathVariable UUID proposalId,
            @RequestParam(required = false) Integer totalMembers,
            @RequestParam(required = false) Double voteQuorum) {
        
        // 파라미터가 제공된 경우 정확한 집계 수행
        if (totalMembers != null && voteQuorum != null) {
            voteService.tallyVotes(proposalId, totalMembers, voteQuorum);
        } else {
            // 파라미터 없으면 간단 버전 사용 (임시)
            voteService.tallyVotesSimple(proposalId);
        }
        
        return ResponseEntity.ok().build();
    }
}

