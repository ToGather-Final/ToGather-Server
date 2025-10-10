package com.example.vote_service.service;

import com.example.vote_service.model.*;
import com.example.vote_service.repository.HistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HistoryServiceTest {

    @Mock
    private HistoryRepository historyRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private HistoryService historyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("투표 생성 히스토리 생성 - 성공")
    void createVoteCreatedHistory_Success() throws Exception {
        // Given
        UUID groupId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        String proposalName = "삼성전자 100주 매수";
        String proposerName = "김투자";

        String expectedPayload = "{\"proposalId\":\"" + proposalId + "\",\"proposalName\":\"" + proposalName + "\",\"proposerName\":\"" + proposerName + "\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedPayload);

        History savedHistory = History.create(groupId, HistoryCategory.VOTE, HistoryType.VOTE_CREATED, "투표가 생성되었습니다: " + proposalName, expectedPayload);
        savedHistory.setHistoryIdForTest(UUID.randomUUID());
        when(historyRepository.save(any(History.class))).thenReturn(savedHistory);

        // When
        historyService.createVoteCreatedHistory(groupId, proposalId, proposalName, proposerName);

        // Then
        verify(objectMapper).writeValueAsString(any());
        verify(historyRepository).save(any(History.class));
    }

    @Test
    @DisplayName("투표 가결 히스토리 생성 - 성공")
    void createVoteApprovedHistory_Success() throws Exception {
        // Given
        UUID groupId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        String scheduledAt = "2024-01-01 15:00:00";
        String side = "BUY";
        String stockName = "삼성전자";
        Integer shares = 100;
        Integer unitPrice = 70000;
        String currency = "KRW";

        String expectedPayload = "{\"proposalId\":\"" + proposalId + "\",\"scheduledAt\":\"" + scheduledAt + "\",\"side\":\"" + side + "\",\"stockName\":\"" + stockName + "\",\"shares\":" + shares + ",\"unitPrice\":" + unitPrice + ",\"currency\":\"" + currency + "\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedPayload);

        History savedHistory = History.create(groupId, HistoryCategory.VOTE, HistoryType.VOTE_APPROVED, "투표가 가결되었습니다: " + stockName + " " + shares + "주", expectedPayload);
        savedHistory.setHistoryIdForTest(UUID.randomUUID());
        when(historyRepository.save(any(History.class))).thenReturn(savedHistory);

        // When
        historyService.createVoteApprovedHistory(groupId, proposalId, scheduledAt, side, stockName, shares, unitPrice, currency);

        // Then
        verify(objectMapper).writeValueAsString(any());
        verify(historyRepository).save(any(History.class));
    }

    @Test
    @DisplayName("투표 부결 히스토리 생성 - 성공")
    void createVoteRejectedHistory_Success() throws Exception {
        // Given
        UUID groupId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        String proposalName = "테슬라 50주 매수";

        String expectedPayload = "{\"proposalId\":\"" + proposalId + "\",\"proposalName\":\"" + proposalName + "\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedPayload);

        History savedHistory = History.create(groupId, HistoryCategory.VOTE, HistoryType.VOTE_REJECTED, "투표가 부결되었습니다: " + proposalName, expectedPayload);
        savedHistory.setHistoryIdForTest(UUID.randomUUID());
        when(historyRepository.save(any(History.class))).thenReturn(savedHistory);

        // When
        historyService.createVoteRejectedHistory(groupId, proposalId, proposalName);

        // Then
        verify(objectMapper).writeValueAsString(any());
        verify(historyRepository).save(any(History.class));
    }

    @Test
    @DisplayName("JSON 직렬화 오류 - 예외 발생")
    void createVoteCreatedHistory_JsonProcessingException_ThrowsException() throws Exception {
        // Given
        UUID groupId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        String proposalName = "테스트 제안";
        String proposerName = "테스트 사용자";

        when(objectMapper.writeValueAsString(any())).thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("JSON 오류") {});

        // When & Then
        try {
            historyService.createVoteCreatedHistory(groupId, proposalId, proposalName, proposerName);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("히스토리 생성 중 오류가 발생했습니다.");
        }

        verify(objectMapper).writeValueAsString(any());
        verify(historyRepository, never()).save(any(History.class));
    }
}
