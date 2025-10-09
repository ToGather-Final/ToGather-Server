package com.example.vote_service.service;

import com.example.vote_service.model.*;
import com.example.vote_service.repository.HistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * History 서비스
 * - 히스토리 생성, 조회 등의 비즈니스 로직 처리
 */
@Service
@RequiredArgsConstructor
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    /**
     * 투표 생성 히스토리 생성
     */
    @Transactional
    public void createVoteCreatedHistory(UUID groupId, UUID proposalId, String proposalName, String proposerName) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("proposalId", proposalId.toString());
            payload.put("proposalName", proposalName);
            payload.put("proposerName", proposerName);
            
            String payloadJson = objectMapper.writeValueAsString(payload);
            String title = String.format("투표가 생성되었습니다: %s", proposalName);
            
            History history = History.create(
                groupId,
                HistoryCategory.VOTE,
                HistoryType.VOTE_CREATED,
                title,
                payloadJson
            );
            
            historyRepository.save(history);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("히스토리 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 투표 가결 히스토리 생성
     */
    @Transactional
    public void createVoteApprovedHistory(UUID groupId, UUID proposalId, String scheduledAt, 
                                        String side, String stockName, Integer shares, 
                                        Integer unitPrice, String currency) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("proposalId", proposalId.toString());
            payload.put("scheduledAt", scheduledAt);
            payload.put("side", side);
            payload.put("stockName", stockName);
            payload.put("shares", shares);
            payload.put("unitPrice", unitPrice);
            payload.put("currency", currency);
            
            String payloadJson = objectMapper.writeValueAsString(payload);
            String title = String.format("투표가 가결되었습니다: %s %d주", stockName, shares);
            
            History history = History.create(
                groupId,
                HistoryCategory.VOTE,
                HistoryType.VOTE_APPROVED,
                title,
                payloadJson
            );
            
            historyRepository.save(history);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("히스토리 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 투표 부결 히스토리 생성
     */
    @Transactional
    public void createVoteRejectedHistory(UUID groupId, UUID proposalId, String proposalName) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("proposalId", proposalId.toString());
            payload.put("proposalName", proposalName);
            
            String payloadJson = objectMapper.writeValueAsString(payload);
            String title = String.format("투표가 부결되었습니다: %s", proposalName);
            
            History history = History.create(
                groupId,
                HistoryCategory.VOTE,
                HistoryType.VOTE_REJECTED,
                title,
                payloadJson
            );
            
            historyRepository.save(history);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("히스토리 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 그룹의 히스토리 조회
     */
    @Transactional(readOnly = true)
    public List<History> getGroupHistory(UUID groupId) {
        return historyRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
    }

    /**
     * 특정 그룹의 특정 카테고리 히스토리 조회
     */
    @Transactional(readOnly = true)
    public List<History> getGroupHistoryByCategory(UUID groupId, HistoryCategory category) {
        return historyRepository.findByGroupIdAndHistoryCategoryOrderByCreatedAtDesc(groupId, category.name());
    }
}
