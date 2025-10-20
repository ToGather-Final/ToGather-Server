package com.example.vote_service.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryTest {

    @Test
    @DisplayName("History 생성 - 성공")
    void createHistory_Success() {
        // Given
        UUID groupId = UUID.randomUUID();
        HistoryCategory category = HistoryCategory.VOTE;
        HistoryType type = HistoryType.VOTE_CREATED_BUY;
        String title = "투표가 생성되었습니다: 삼성전자 100주 매수";
        String payload = "{\"proposalId\":\"123\",\"proposalName\":\"삼성전자 100주 매수\"}";

        // When
        History history = History.create(groupId, category, type, title, payload);

        // Then
        assertThat(history.getGroupId()).isEqualTo(groupId);
        assertThat(history.getHistoryCategory()).isEqualTo(category);
        assertThat(history.getHistoryType()).isEqualTo(type);
        assertThat(history.getTitle()).isEqualTo(title);
        assertThat(history.getPayload()).isEqualTo(payload);
        assertThat(history.getDate()).isNotNull();
        assertThat(history.getDate()).hasSize(19); // YYYY-MM-DD HH:mm:ss
    }

    @Test
    @DisplayName("History 생성 - VOTE_APPROVED")
    void createHistory_VoteApproved_Success() {
        // Given
        UUID groupId = UUID.randomUUID();
        HistoryCategory category = HistoryCategory.VOTE;
        HistoryType type = HistoryType.VOTE_APPROVED;
        String title = "투표가 가결되었습니다: 삼성전자 100주";
        String payload = "{\"proposalId\":\"123\",\"scheduledAt\":\"2024-01-01 15:00:00\",\"side\":\"BUY\",\"stockName\":\"삼성전자\",\"shares\":100,\"unitPrice\":70000,\"currency\":\"KRW\"}";

        // When
        History history = History.create(groupId, category, type, title, payload);

        // Then
        assertThat(history.getGroupId()).isEqualTo(groupId);
        assertThat(history.getHistoryCategory()).isEqualTo(HistoryCategory.VOTE);
        assertThat(history.getHistoryType()).isEqualTo(HistoryType.VOTE_APPROVED);
        assertThat(history.getTitle()).isEqualTo(title);
        assertThat(history.getPayload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("History 생성 - VOTE_REJECTED")
    void createHistory_VoteRejected_Success() {
        // Given
        UUID groupId = UUID.randomUUID();
        HistoryCategory category = HistoryCategory.VOTE;
        HistoryType type = HistoryType.VOTE_REJECTED;
        String title = "투표가 부결되었습니다: 테슬라 50주 매수";
        String payload = "{\"proposalId\":\"456\",\"proposalName\":\"테슬라 50주 매수\"}";

        // When
        History history = History.create(groupId, category, type, title, payload);

        // Then
        assertThat(history.getGroupId()).isEqualTo(groupId);
        assertThat(history.getHistoryCategory()).isEqualTo(HistoryCategory.VOTE);
        assertThat(history.getHistoryType()).isEqualTo(HistoryType.VOTE_REJECTED);
        assertThat(history.getTitle()).isEqualTo(title);
        assertThat(history.getPayload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("History 생성 - 다른 카테고리")
    void createHistory_OtherCategory_Success() {
        // Given
        UUID groupId = UUID.randomUUID();
        HistoryCategory category = HistoryCategory.TRADE;
        HistoryType type = HistoryType.TRADE_EXECUTED;
        String title = "매매가 완료되었습니다: 삼성전자 100주";
        String payload = "{\"tradeId\":\"789\",\"stockName\":\"삼성전자\",\"shares\":100}";

        // When
        History history = History.create(groupId, category, type, title, payload);

        // Then
        assertThat(history.getGroupId()).isEqualTo(groupId);
        assertThat(history.getHistoryCategory()).isEqualTo(HistoryCategory.TRADE);
        assertThat(history.getHistoryType()).isEqualTo(HistoryType.TRADE_EXECUTED);
        assertThat(history.getTitle()).isEqualTo(title);
        assertThat(history.getPayload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("테스트용 ID 설정 - 성공")
    void setHistoryIdForTest_Success() {
        // Given
        UUID groupId = UUID.randomUUID();
        History history = History.create(groupId, HistoryCategory.VOTE, HistoryType.VOTE_CREATED_BUY, "테스트", "{}");
        UUID testId = UUID.randomUUID();

        // When
        history.setHistoryIdForTest(testId);

        // Then
        assertThat(history.getHistoryId()).isEqualTo(testId);
    }
}
