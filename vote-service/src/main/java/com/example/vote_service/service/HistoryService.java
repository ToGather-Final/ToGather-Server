package com.example.vote_service.service;

import com.example.vote_service.dto.*;
import com.example.vote_service.dto.payload.*;
import com.example.vote_service.model.*;
import com.example.vote_service.repository.HistoryRepository;
import com.example.vote_service.repository.GroupMembersRepository;
import com.example.vote_service.security.JwtUtil;
import com.example.vote_service.event.HistoryCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * History 서비스
 * - 히스토리 생성 및 조회 비즈니스 로직 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final GroupMembersRepository groupMembersRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 투표 생성 히스토리 생성 (사용자 ID 기반)
     */
    @Transactional
    public void createVoteCreatedHistory(UUID userId, UUID proposalId, String proposalName, String proposerName, Integer price, Integer quantity, HistoryType historyType) {
        try {
            // 사용자가 속한 그룹 ID 조회 (단일 그룹)
            Optional<UUID> groupIdOpt = groupMembersRepository.findFirstGroupIdByUserId(userId);
            if (groupIdOpt.isEmpty()) {
                throw new IllegalArgumentException("사용자가 속한 그룹이 없습니다.");
            }
            
            UUID groupId = groupIdOpt.get();
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("proposalId", proposalId.toString());
            payload.put("proposalName", proposalName);
            payload.put("proposerName", proposerName);
            if (price != null) {
                payload.put("price", price);
            }
            if (quantity != null) {
                payload.put("quantity", quantity);
            }
            
            String payloadJson = objectMapper.writeValueAsString(payload);
            String title = "투표가 생성되었습니다";
            
            History history = History.create(
                groupId,
                HistoryCategory.VOTE,
                historyType,
                title,
                payloadJson,
                price,
                quantity
            );
            
            historyRepository.save(history);
            
            // 🔥 히스토리 생성 이벤트 발행 - 자동으로 알림 전송됨
            eventPublisher.publishEvent(new HistoryCreatedEvent(history));
            
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
                                        Integer unitPrice, String currency, UUID stockId) {
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
            String title = "투표가 가결되었습니다";
            
            History history = History.create(
                groupId,
                HistoryCategory.VOTE,
                HistoryType.VOTE_APPROVED,
                title,
                payloadJson
            );
            
            // 주식 ID 설정 (DB의 stock_id 컬럼에 저장)
            history.setStockId(stockId);
            
            historyRepository.save(history);
            
            // 🔥 히스토리 생성 이벤트 발행 - 자동으로 알림 전송됨
            eventPublisher.publishEvent(new HistoryCreatedEvent(history));
            
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
            String title = "투표가 부결되었습니다";
            
            History history = History.create(
                groupId,
                HistoryCategory.VOTE,
                HistoryType.VOTE_REJECTED,
                title,
                payloadJson
            );
            
            historyRepository.save(history);
            
            // 🔥 히스토리 생성 이벤트 발행 - 자동으로 알림 전송됨
            eventPublisher.publishEvent(new HistoryCreatedEvent(history));
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("히스토리 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자의 그룹 히스토리 조회
     */
    @Transactional(readOnly = true)
    public GetHistoryResponse getHistoryByUserId(UUID userId, int page, int size) {
        try {
            // 사용자가 속한 그룹 ID 조회
            Optional<UUID> groupIdOpt = groupMembersRepository.findFirstGroupIdByUserId(userId);
            if (groupIdOpt.isEmpty()) {
                log.warn("사용자가 속한 그룹이 없습니다 - userId: {}", userId);
                return new GetHistoryResponse(List.of(), null);
            }
            
            UUID groupId = groupIdOpt.get();
            log.info("사용자 그룹 히스토리 조회 - userId: {}, groupId: {}, page: {}, size: {}", 
                    userId, groupId, page, size);
            
            // 페이징 처리
            Pageable pageable = PageRequest.of(page, size);
            Page<History> historyPage = historyRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable);
            
            // DTO 변환
            List<HistoryDTO> historyDTOs = historyPage.getContent().stream()
                    .map(this::convertToHistoryDTO)
                    .collect(Collectors.toList());
            
            // 다음 커서 생성 (마지막 히스토리 ID)
            String nextCursor = null;
            if (historyPage.hasNext()) {
                History lastHistory = historyPage.getContent().get(historyPage.getContent().size() - 1);
                nextCursor = lastHistory.getHistoryId().toString();
            }
            
            log.info("히스토리 조회 완료 - 총 {}개, 다음 커서: {}", historyDTOs.size(), nextCursor);
            return new GetHistoryResponse(historyDTOs, nextCursor);
            
        } catch (Exception e) {
            log.error("히스토리 조회 중 오류 발생 - userId: {}, error: {}", userId, e.getMessage(), e);
            throw new RuntimeException("히스토리 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자의 그룹 히스토리 전체 조회 (페이징 없음)
     */
    @Transactional(readOnly = true)
    public GetHistoryResponse getAllHistoryByUserId(UUID userId) {
        try {
            // 사용자가 속한 그룹 ID 조회
            Optional<UUID> groupIdOpt = groupMembersRepository.findFirstGroupIdByUserId(userId);
            if (groupIdOpt.isEmpty()) {
                log.warn("사용자가 속한 그룹이 없습니다 - userId: {}", userId);
                return new GetHistoryResponse(List.of(), null);
            }
            
            UUID groupId = groupIdOpt.get();
            log.info("사용자 그룹 히스토리 전체 조회 - userId: {}, groupId: {}", userId, groupId);
            
            // 전체 히스토리 조회 (페이징 없음)
            List<History> histories = historyRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
            
            // DTO 변환
            List<HistoryDTO> historyDTOs = histories.stream()
                    .map(this::convertToHistoryDTO)
                    .collect(Collectors.toList());
            
            log.info("히스토리 전체 조회 완료 - userId: {}, 총 {}개", userId, historyDTOs.size());
            return new GetHistoryResponse(historyDTOs, null); // 전체 조회이므로 nextCursor는 null
            
        } catch (Exception e) {
            log.error("히스토리 전체 조회 중 오류 발생 - userId: {}, error: {}", userId, e.getMessage(), e);
            throw new RuntimeException("히스토리 전체 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * History 엔티티를 HistoryDTO로 변환
     */
    private HistoryDTO convertToHistoryDTO(History history) {
        try {
            Object payload = parsePayload(history.getPayload(), history.getHistoryType());
            
            return new HistoryDTO(
                    history.getHistoryId(),
                    history.getHistoryCategory(),
                    history.getHistoryType(),
                    history.getTitle(),
                    history.getDate(),
                    payload
            );
        } catch (Exception e) {
            log.error("History DTO 변환 중 오류 - historyId: {}, error: {}", 
                    history.getHistoryId(), e.getMessage(), e);
            // 오류 발생 시 기본 페이로드 사용
            return new HistoryDTO(
                    history.getHistoryId(),
                    history.getHistoryCategory(),
                    history.getHistoryType(),
                    history.getTitle(),
                    history.getDate(),
                    Map.of("error", "페이로드 파싱 실패")
            );
        }
    }

    /**
     * JSON 페이로드를 타입에 맞는 DTO로 파싱
     */
    private Object parsePayload(String payloadJson, HistoryType historyType) {
        if (payloadJson == null || payloadJson.trim().isEmpty()) {
            return Map.of();
        }
        
        try {
            Map<String, Object> payloadMap = objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
            
            switch (historyType) {
                case VOTE_CREATED_BUY:
                case VOTE_CREATED_SELL:
                case VOTE_CREATED_PAY:
                    return new VoteCreatedPayloadDTO(
                            UUID.fromString((String) payloadMap.get("proposalId")),
                            (String) payloadMap.get("proposalName"),
                            (String) payloadMap.get("proposerName")
                    );
                    
                case VOTE_APPROVED:
                    return new VoteApprovedPayloadDTO(
                            UUID.fromString((String) payloadMap.get("proposalId")),
                            (String) payloadMap.get("scheduledAt"),
                            (String) payloadMap.get("side"),
                            (String) payloadMap.get("stockName"),
                            (Integer) payloadMap.get("shares"),
                            (Integer) payloadMap.get("unitPrice"),
                            (String) payloadMap.get("currency")
                    );
                    
                case VOTE_REJECTED:
                    return new VoteRejectedPayloadDTO(
                            UUID.fromString((String) payloadMap.get("proposalId")),
                            (String) payloadMap.get("proposalName")
                    );
                    
                case TRADE_EXECUTED:
                    return new TradeExecutedPayloadDTO(
                            (String) payloadMap.get("side"),
                            (String) payloadMap.get("stockName"),
                            (Integer) payloadMap.get("shares"),
                            (Integer) payloadMap.get("unitPrice"),
                            (Integer) payloadMap.get("accountBalance")
                    );
                    
                case TRADE_FAILED:
                    return new TradeFailedPayloadDTO(
                            (String) payloadMap.get("side"),
                            (String) payloadMap.get("stockName"),
                            (String) payloadMap.get("reason")
                    );
                    
                case CASH_DEPOSIT_COMPLETED:
                    return new CashDepositCompletedPayloadDTO(
                            (String) payloadMap.get("depositorName"),
                            (Integer) payloadMap.get("amount"),
                            (Integer) payloadMap.get("accountBalance")
                    );
                    
                case PAY_CHARGE_COMPLETED:
                    return new PayChargeCompletedPayloadDTO(
                            (Integer) payloadMap.get("amount"),
                            (Integer) payloadMap.get("accountBalance")
                    );
                    
                case GOAL_ACHIEVED:
                    return new GoalAchievedPayloadDTO(
                            (Integer) payloadMap.get("targetAmount")
                    );
                    
                default:
                    log.warn("알 수 없는 히스토리 타입: {}", historyType);
                    return payloadMap; // 기본 Map 반환
            }
        } catch (Exception e) {
            log.error("페이로드 파싱 중 오류 - payloadJson: {}, historyType: {}, error: {}", 
                    payloadJson, historyType, e.getMessage(), e);
            return Map.of("error", "페이로드 파싱 실패");
        }
    }

}
