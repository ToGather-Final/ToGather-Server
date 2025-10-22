package com.example.vote_service.controller;

import com.example.vote_service.dto.*;
import com.example.vote_service.model.Proposal;
import com.example.vote_service.service.ProposalService;
import com.example.vote_service.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Vote Controller
 * - 투표 관련 API 엔드포인트
 */
@RestController
@RequestMapping("/vote")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "투표 관리", description = "투표 제안, 참여, 집계 관련 API")
public class VoteController {

    private final ProposalService proposalService;
    private final VoteService voteService;

    /**
     * GET /vote - 투표 목록 조회 (전체/예매/페이)
     * - 사용자의 그룹을 자동으로 조회하여 해당 그룹의 투표 목록 반환
     * - view 파라미터로 필터링 (view=TRADE, view=PAY 등)
     */
    @Operation(summary = "투표 목록 조회", description = "사용자 그룹의 투표 목록을 조회합니다. view 파라미터로 카테고리별 필터링이 가능합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "투표 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<List<ProposalResponse>> getProposals(
            @Parameter(description = "카테고리 필터 (TRADE, PAY 등)") @RequestParam(required = false) String view,
            Authentication authentication) {
        
        UUID userId = (UUID) authentication.getPrincipal();
        
        // 사용자의 그룹 ID 자동 조회
        UUID groupId = proposalService.getUserGroupId(userId);
        List<Proposal> proposals = proposalService.getProposalsByGroup(groupId);
        
        // view 파라미터로 필터링
        if (view != null && !view.isBlank()) {
            proposals = proposals.stream()
                    .filter(p -> p.getCategory().name().equalsIgnoreCase(view))
                    .collect(Collectors.toList());
        }
        
        // 성능 최적화: 배치 조회로 N+1 쿼리 문제 해결
        List<UUID> proposalIds = proposals.stream()
                .map(Proposal::getProposalId)
                .collect(Collectors.toList());
        
        // 한 번에 모든 투표 수 조회
        Map<UUID, Long> approveCounts = voteService.getApproveVoteCounts(proposalIds);
        Map<UUID, Long> rejectCounts = voteService.getRejectVoteCounts(proposalIds);
        Map<UUID, com.example.vote_service.model.VoteChoice> userVotes = voteService.getUserVoteChoices(userId, proposalIds);
        
        List<ProposalResponse> response = proposals.stream()
                .map(p -> {
                    // 배치 조회 결과에서 값 가져오기 (없으면 0)
                    int approveCount = approveCounts.getOrDefault(p.getProposalId(), 0L).intValue();
                    int rejectCount = rejectCounts.getOrDefault(p.getProposalId(), 0L).intValue();
                    var myVote = userVotes.get(p.getProposalId());
                    
                    // 제안자 이름은 Proposal 엔티티에서 가져오기
                    String proposerName = p.getProposerName();
                    
                    // 날짜 포맷팅 (yyyy-MM-dd)
                    String date = p.getOpenAt().toLocalDate().toString();
                    
                    // 투표 마감 시간 포맷팅 (yyyy-MM-dd HH시 mm분)
                    String closeAt = p.getCloseAt() != null
                            ? formatToKoreaTime(p.getCloseAt())
                            : null;
                    
                    return new ProposalResponse(
                            p.getProposalId(),
                            p.getProposalName(),
                            proposerName,
                            p.getCategory(),
                            p.getAction(),
                            p.getPayload(),
                            p.getStatus(),
                            date,
                            closeAt,
                            approveCount,
                            rejectCount,
                            myVote
                    );
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    private String formatToKoreaTime(LocalDateTime localDateTime) {
        try{
            ZoneId serverZone = ZoneId.systemDefault();
            ZoneId koreaZone = ZoneId.of("Asia/Seoul");

            ZonedDateTime serverTime = localDateTime.atZone(serverZone);
            ZonedDateTime koreaTime = serverTime.withZoneSameInstant(koreaZone);

            return String.format("%04d-%02d-%02d %02d시 %02d분",
                    koreaTime.getYear(),
                    koreaTime.getMonthValue(),
                    koreaTime.getDayOfMonth(),
                    koreaTime.getHour(),
                    koreaTime.getMinute());
        } catch (Exception e) {
            log.error("시간 포맷팅 실패 - localDateTime: {}, error: {}", localDateTime, e.getMessage());
            // 실패 시 원본 시간 그대로 반환
            return String.format("%04d-%02d-%02d %02d시 %02d분",
                    localDateTime.getYear(),
                    localDateTime.getMonthValue(),
                    localDateTime.getDayOfMonth(),
                    localDateTime.getHour(),
                    localDateTime.getMinute());
        }
    }

    /**
     * POST /vote - 투표 제안
     * (예수, 마도, 예수금 총칭, 페이 머니 설정)
     */
    @Operation(summary = "투표 제안 생성", description = "새로운 투표 제안을 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "투표 제안 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<UUID> createProposal(
            @Parameter(description = "투표 제안 생성 요청 데이터", required = true) @Valid @RequestBody ProposalCreateRequest request,
            Authentication authentication) {
        
        UUID userId = (UUID) authentication.getPrincipal();
        UUID proposalId = proposalService.createProposal(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(proposalId);
    }

    /**
     * POST /vote/{proposalId} - 각각의 개인이 투표 (찬성/반대)
     */
    @Operation(summary = "투표 참여", description = "특정 제안에 대해 찬성/반대 투표를 합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "투표 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 이미 투표함"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "제안을 찾을 수 없음")
    })
    @PostMapping("/{proposalId}")
    public ResponseEntity<Void> vote(
            @Parameter(description = "제안 ID", required = true) @PathVariable UUID proposalId,
            @Parameter(description = "투표 요청 데이터", required = true) @Valid @RequestBody VoteRequest request,
            Authentication authentication) {
        
        UUID userId = (UUID) authentication.getPrincipal();
        voteService.vote(userId, proposalId, request);
        
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "투표 집계 및 종료", description = "투표를 집계하고 결과에 따라 투표를 종료합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "투표 집계 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "404", description = "투표를 찾을 수 없음")
    })
    @PostMapping("/{proposalId}/tally")
    public ResponseEntity<Void> tallyVotes(
            @PathVariable UUID proposalId,
            @RequestParam(required = false) Integer totalMembers,
            @RequestParam(required = false) Integer voteQuorum) {
        
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

