package com.example.vote_service.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProposalTest {

    @Test
    @DisplayName("제안 생성")
    void createProposal_Success() {
        // Given
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime closeAt = LocalDateTime.now().plusHours(24);

        // When
        Proposal proposal = Proposal.create(
                groupId,
                userId,
                "테스트 제안",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                closeAt
        );

        // Then
        assertThat(proposal).isNotNull();
        assertThat(proposal.getProposalName()).isEqualTo("테스트 제안");
        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.OPEN);
        assertThat(proposal.getCategory()).isEqualTo(ProposalCategory.TRADE);
    }

    @Test
    @DisplayName("제안 승인")
    void approve_Success() {
        // Given
        Proposal proposal = Proposal.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "테스트 제안",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().plusHours(24)
        );

        // When
        proposal.approve();

        // Then
        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.APPROVED);
        assertThat(proposal.getCloseAt()).isNotNull();
    }

    @Test
    @DisplayName("제안 거부")
    void reject_Success() {
        // Given
        Proposal proposal = Proposal.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "테스트 제안",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().plusHours(24)
        );

        // When
        proposal.reject();

        // Then
        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.REJECTED);
    }

    @Test
    @DisplayName("이미 종료된 제안 승인 시도 - 예외 발생")
    void approve_AlreadyClosed_ThrowsException() {
        // Given
        Proposal proposal = Proposal.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "테스트 제안",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().plusHours(24)
        );
        proposal.approve(); // 이미 승인됨

        // When & Then
        assertThatThrownBy(proposal::approve)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("열린 제안만 승인할 수 있습니다.");
    }

    @Test
    @DisplayName("제안 진행 중 확인")
    void isOpen_OpenProposal_ReturnsTrue() {
        // Given
        Proposal proposal = Proposal.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "테스트 제안",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().plusHours(24)
        );

        // When & Then
        assertThat(proposal.isOpen()).isTrue();
    }

    @Test
    @DisplayName("마감 시간 지났는지 확인")
    void isExpired_ExpiredProposal_ReturnsTrue() {
        // Given
        Proposal proposal = Proposal.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "테스트 제안",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().minusHours(1) // 1시간 전에 마감
        );

        // When & Then
        assertThat(proposal.isExpired()).isTrue();
    }

    @Test
    @DisplayName("투표 가능한 상태 확인 - 진행중 + 마감 전")
    void canVote_OpenAndNotExpired_ReturnsTrue() {
        // Given
        Proposal proposal = Proposal.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "테스트 제안",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().plusHours(24)
        );

        // When & Then
        assertThat(proposal.canVote()).isTrue();
    }

    @Test
    @DisplayName("투표 불가능한 상태 - 마감됨")
    void canVote_Expired_ReturnsFalse() {
        // Given
        Proposal proposal = Proposal.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "테스트 제안",
                ProposalCategory.TRADE,
                ProposalAction.BUY,
                "{}",
                LocalDateTime.now().minusHours(1)
        );

        // When & Then
        assertThat(proposal.canVote()).isFalse();
    }
}

