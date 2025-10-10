package com.example.vote_service.service;

import com.example.vote_service.dto.VoteRequest;
import com.example.vote_service.model.*;
import com.example.vote_service.repository.VoteRepository;
import com.example.vote_service.repository.GroupMembersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VoteServiceTest {

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private ProposalService proposalService;

    @Mock
    private GroupMembersRepository groupMembersRepository;

    @Mock
    private HistoryService historyService;

    @InjectMocks
    private VoteService voteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("투표 생성 - 처음 투표하는 경우")
    void vote_NewVote_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        VoteRequest request = new VoteRequest(VoteChoice.AGREE);

        Proposal proposal = Proposal.create(
                groupId,
                userId,
                "테스트 제안",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().plusMinutes(5)
        );

        // 그룹 멤버십 검증 모킹
        when(groupMembersRepository.existsByUserIdAndGroupId(userId, groupId)).thenReturn(true);
        when(proposalService.getProposal(proposalId)).thenReturn(proposal);
        when(voteRepository.findByProposalIdAndUserId(proposalId, userId))
                .thenReturn(Optional.empty());

        when(voteRepository.save(any(Vote.class))).thenAnswer(invocation -> {
            Vote vote = invocation.getArgument(0);
            // JPA가 실제로 하는 것처럼 UUID 설정
            vote.setVoteIdForTest(UUID.randomUUID());
            return vote;
        });

        // When
        UUID voteId = voteService.vote(userId, proposalId, request);

        // Then
        assertThat(voteId).isNotNull();
        verify(groupMembersRepository).existsByUserIdAndGroupId(userId, groupId);
        verify(voteRepository).save(any(Vote.class));
    }

    @Test
    @DisplayName("투표 변경 - 이미 투표한 경우")
    void vote_ChangeVote_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        VoteRequest request = new VoteRequest(VoteChoice.DISAGREE);

        Proposal proposal = Proposal.create(
                groupId,
                userId,
                "테스트 제안",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().plusMinutes(5)
        );

        // 그룹 멤버십 검증 모킹
        when(groupMembersRepository.existsByUserIdAndGroupId(userId, groupId)).thenReturn(true);
        when(proposalService.getProposal(proposalId)).thenReturn(proposal);

        Vote existingVote = Vote.create(proposalId, userId, VoteChoice.AGREE);
        existingVote.setVoteIdForTest(UUID.randomUUID()); // 기존 투표에 UUID 설정
        when(voteRepository.findByProposalIdAndUserId(proposalId, userId))
                .thenReturn(Optional.of(existingVote));

        // When
        UUID voteId = voteService.vote(userId, proposalId, request);

        // Then
        assertThat(voteId).isNotNull();
        assertThat(existingVote.getChoice()).isEqualTo(VoteChoice.DISAGREE);
        verify(groupMembersRepository).existsByUserIdAndGroupId(userId, groupId);
        verify(voteRepository, never()).save(any()); // 기존 엔티티 수정이므로 save 호출 안함
    }

    @Test
    @DisplayName("투표 실패 - 그룹 멤버가 아님")
    void vote_Fail_NotGroupMember() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        VoteRequest request = new VoteRequest(VoteChoice.AGREE);

        Proposal proposal = Proposal.create(
                groupId,
                userId,
                "테스트 제안",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().plusMinutes(5)
        );

        // 그룹 멤버가 아님
        when(groupMembersRepository.existsByUserIdAndGroupId(userId, groupId)).thenReturn(false);
        when(proposalService.getProposal(proposalId)).thenReturn(proposal);

        // When & Then
        assertThatThrownBy(() -> voteService.vote(userId, proposalId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 그룹의 멤버가 아닙니다.");

        verify(groupMembersRepository).existsByUserIdAndGroupId(userId, groupId);
        verify(voteRepository, never()).save(any(Vote.class));
    }

    @Test
    @DisplayName("종료된 제안에 투표 시도 - 예외 발생")
    void vote_ClosedProposal_ThrowsException() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        VoteRequest request = new VoteRequest(VoteChoice.AGREE);

        Proposal closedProposal = Proposal.create(
                groupId,
                userId,
                "테스트 제안",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().minusHours(1) // 이미 마감
        );

        // 그룹 멤버십 검증 모킹
        when(groupMembersRepository.existsByUserIdAndGroupId(userId, groupId)).thenReturn(true);
        when(proposalService.getProposal(proposalId)).thenReturn(closedProposal);

        // When & Then
        assertThatThrownBy(() -> voteService.vote(userId, proposalId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("투표 마감 시간이 지났습니다.");
    }

    @Test
    @DisplayName("투표 집계 - 찬성이 반대보다 많으면 승인")
    void tallyVotes_MoreAgree_Approved() {
        // Given
        UUID proposalId = UUID.randomUUID();
        Proposal proposal = Proposal.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "테스트 제안",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().minusHours(1) // 마감됨
        );

        when(proposalService.getProposal(proposalId)).thenReturn(proposal);
        when(voteRepository.countByProposalIdAndChoice(proposalId, VoteChoice.AGREE))
                .thenReturn(3L);
        when(voteRepository.countByProposalIdAndChoice(proposalId, VoteChoice.DISAGREE))
                .thenReturn(1L);

        // When
        voteService.tallyVotes(proposalId, 5, 3); // 정족수 3명

        // Then
        // 3명 찬성 >= 3명 정족수 && 3 > 1 → 승인
        verify(proposalService).approveProposal(proposalId);
        verify(historyService).createVoteApprovedHistory(
                eq(proposal.getGroupId()),
                eq(proposalId),
                eq("2024-01-01 15:00:00"),
                eq("BUY"),
                eq("삼성전자"),
                eq(100),
                eq(70000),
                eq("KRW")
        );
    }

    @Test
    @DisplayName("투표 집계 - 정족수 미달로 부결")
    void tallyVotes_QuorumNotMet_Rejected() {
        // Given
        UUID proposalId = UUID.randomUUID();
        Proposal proposal = Proposal.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "테스트 제안",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().minusHours(1)
        );

        when(proposalService.getProposal(proposalId)).thenReturn(proposal);
        when(voteRepository.countByProposalIdAndChoice(proposalId, VoteChoice.AGREE))
                .thenReturn(2L);
        when(voteRepository.countByProposalIdAndChoice(proposalId, VoteChoice.DISAGREE))
                .thenReturn(1L);

        // When
        voteService.tallyVotes(proposalId, 5, 3); // 정족수 3명

        // Then
        // 2명 찬성 < 3명 정족수 → 부결
        verify(proposalService).rejectProposal(proposalId);
        verify(historyService).createVoteRejectedHistory(
                eq(proposal.getGroupId()),
                eq(proposalId),
                eq("테스트 제안")
        );
    }

    @Test
    @DisplayName("투표 카운트 조회")
    void countVotes_Success() {
        // Given
        UUID proposalId = UUID.randomUUID();
        when(voteRepository.countByProposalIdAndChoice(proposalId, VoteChoice.AGREE))
                .thenReturn(5L);
        when(voteRepository.countByProposalIdAndChoice(proposalId, VoteChoice.DISAGREE))
                .thenReturn(3L);
        when(voteRepository.countByProposalId(proposalId))
                .thenReturn(8L);

        // When
        long agreeCount = voteService.countApproveVotes(proposalId);
        long disagreeCount = voteService.countRejectVotes(proposalId);
        long totalCount = voteService.countTotalVotes(proposalId);

        // Then
        assertThat(agreeCount).isEqualTo(5);
        assertThat(disagreeCount).isEqualTo(3);
        assertThat(totalCount).isEqualTo(8);
    }
}

