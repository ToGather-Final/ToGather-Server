package com.example.vote_service.service;

import com.example.vote_service.dto.ProposalCreateRequest;
import com.example.vote_service.model.*;
import com.example.vote_service.repository.ProposalRepository;
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

class ProposalServiceTest {

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private GroupMembersRepository groupMembersRepository;

    @InjectMocks
    private ProposalService proposalService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("제안 생성 성공")
    void createProposal_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        ProposalCreateRequest request = new ProposalCreateRequest(
                groupId,
                "삼성전자 100주 매수",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{\"stockCode\":\"005930\",\"quantity\":100}"
        );

        Proposal savedProposal = Proposal.create(
                groupId,
                userId,
                "삼성전자 100주 매수",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{\"stockCode\":\"005930\",\"quantity\":100}",
                LocalDateTime.now().plusMinutes(5)
        );

        // 그룹 멤버십 검증 모킹
        when(groupMembersRepository.existsByUserIdAndGroupId(userId, groupId)).thenReturn(true);
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(invocation -> {
            Proposal proposal = invocation.getArgument(0);
            // JPA가 실제로 하는 것처럼 UUID 설정
            proposal.setProposalIdForTest(UUID.randomUUID());
            return proposal;
        });

        // When
        UUID proposalId = proposalService.createProposal(userId, request);

        // Then
        assertThat(proposalId).isNotNull();
        verify(groupMembersRepository).existsByUserIdAndGroupId(userId, groupId);
        verify(proposalRepository).save(any(Proposal.class));
    }

    @Test
    @DisplayName("제안 생성 실패 - 그룹 멤버가 아님")
    void createProposal_Fail_NotGroupMember() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        ProposalCreateRequest request = new ProposalCreateRequest(
                groupId,
                "삼성전자 100주 매수",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{\"stockCode\":\"005930\",\"quantity\":100}"
        );

        // 그룹 멤버가 아님
        when(groupMembersRepository.existsByUserIdAndGroupId(userId, groupId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> proposalService.createProposal(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 그룹의 멤버가 아닙니다.");

        verify(groupMembersRepository).existsByUserIdAndGroupId(userId, groupId);
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    @DisplayName("제안 조회 - 존재하는 제안")
    void getProposal_ExistingProposal_ReturnsProposal() {
        // Given
        UUID proposalId = UUID.randomUUID();
        Proposal proposal = Proposal.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "테스트 제안",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().plusHours(24)
        );
        proposal.setProposalIdForTest(proposalId); // 테스트용 ID 설정

        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        // When
        Proposal found = proposalService.getProposal(proposalId);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getProposalName()).isEqualTo("테스트 제안");
        verify(proposalRepository).findById(proposalId);
    }

    @Test
    @DisplayName("제안 조회 - 존재하지 않는 제안")
    void getProposal_NonExistingProposal_ThrowsException() {
        // Given
        UUID proposalId = UUID.randomUUID();
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> proposalService.getProposal(proposalId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("제안을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("제안 승인 처리")
    void approveProposal_Success() {
        // Given
        UUID proposalId = UUID.randomUUID();
        Proposal proposal = Proposal.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "테스트 제안",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().plusHours(24)
        );
        proposal.setProposalIdForTest(proposalId); // 테스트용 ID 설정

        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        // When
        proposalService.approveProposal(proposalId);

        // Then
        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.APPROVED);
    }
}

