package com.example.vote_service.service;

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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class VoteServiceHistoryTest {

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
    @DisplayName("투표 가결 시 히스토리 생성 - 성공")
    void tallyVotes_Approved_CreatesHistory_Success() {
        // Given
        UUID proposalId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        int totalMembers = 5;
        int voteQuorum = 3;

        Proposal proposal = Proposal.create(
                groupId,
                UUID.randomUUID(),
                "삼성전자 100주 매수",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().minusHours(1) // 마감됨
        );
        proposal.setProposalIdForTest(proposalId);

        when(proposalService.getProposal(proposalId)).thenReturn(proposal);
        when(voteRepository.countByProposalIdAndChoice(proposalId, VoteChoice.AGREE)).thenReturn(4L);
        when(voteRepository.countByProposalIdAndChoice(proposalId, VoteChoice.DISAGREE)).thenReturn(1L);

        // When
        voteService.tallyVotes(proposalId, totalMembers, voteQuorum);

        // Then
        verify(proposalService).approveProposal(proposalId);
        verify(historyService).createVoteApprovedHistory(
                eq(groupId),
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
    @DisplayName("투표 부결 시 히스토리 생성 - 성공")
    void tallyVotes_Rejected_CreatesHistory_Success() {
        // Given
        UUID proposalId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        int totalMembers = 5;
        int voteQuorum = 3;

        Proposal proposal = Proposal.create(
                groupId,
                UUID.randomUUID(),
                "테슬라 50주 매수",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().minusHours(1) // 마감됨
        );
        proposal.setProposalIdForTest(proposalId);

        when(proposalService.getProposal(proposalId)).thenReturn(proposal);
        when(voteRepository.countByProposalIdAndChoice(proposalId, VoteChoice.AGREE)).thenReturn(2L);
        when(voteRepository.countByProposalIdAndChoice(proposalId, VoteChoice.DISAGREE)).thenReturn(3L);

        // When
        voteService.tallyVotes(proposalId, totalMembers, voteQuorum);

        // Then
        verify(proposalService).rejectProposal(proposalId);
        verify(historyService).createVoteRejectedHistory(
                eq(groupId),
                eq(proposalId),
                eq("테슬라 50주 매수")
        );
    }

    @Test
    @DisplayName("투표 부결 시 히스토리 생성 - 정족수 미달")
    void tallyVotes_Rejected_QuorumNotMet_CreatesHistory_Success() {
        // Given
        UUID proposalId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        int totalMembers = 5;
        int voteQuorum = 3;

        Proposal proposal = Proposal.create(
                groupId,
                UUID.randomUUID(),
                "애플 30주 매수",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().minusHours(1) // 마감됨
        );
        proposal.setProposalIdForTest(proposalId);

        when(proposalService.getProposal(proposalId)).thenReturn(proposal);
        when(voteRepository.countByProposalIdAndChoice(proposalId, VoteChoice.AGREE)).thenReturn(2L);
        when(voteRepository.countByProposalIdAndChoice(proposalId, VoteChoice.DISAGREE)).thenReturn(1L);

        // When
        voteService.tallyVotes(proposalId, totalMembers, voteQuorum);

        // Then
        verify(proposalService).rejectProposal(proposalId);
        verify(historyService).createVoteRejectedHistory(
                eq(groupId),
                eq(proposalId),
                eq("애플 30주 매수")
        );
    }
}
