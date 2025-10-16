package com.example.vote_service.service;

import com.example.vote_service.repository.GroupMembersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 실시간 알림 서비스
 * - SSE 연결 관리 및 그룹 멤버들에게 알림 전송
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    // 🔥 사용자별 SSE 연결 관리 (userId -> SseEmitter)
    private final Map<UUID, SseEmitter> userEmitters = new ConcurrentHashMap<>();
    
    private final GroupMembersRepository groupMembersRepository;
    
    /**
     * 사용자 SSE 연결 등록
     */
    public void registerUserConnection(UUID userId, SseEmitter emitter) {
        userEmitters.put(userId, emitter);
        
        // 연결 완료 시 정리
        emitter.onCompletion(() -> {
            userEmitters.remove(userId);
            log.info("🔌 SSE 연결 완료 - userId: {}, 남은 연결: {}개", userId, userEmitters.size());
        });
        
        // 연결 타임아웃 시 정리
        emitter.onTimeout(() -> {
            userEmitters.remove(userId);
            log.info("⏰ SSE 연결 타임아웃 - userId: {}, 남은 연결: {}개", userId, userEmitters.size());
        });
        
        // 연결 오류 시 정리
        emitter.onError((ex) -> {
            userEmitters.remove(userId);
            log.error("❌ SSE 연결 오류 - userId: {}, error: {}, 남은 연결: {}개", 
                    userId, ex.getMessage(), userEmitters.size());
        });
        
        log.info("🔌 SSE 연결 등록 - userId: {}, 총 연결: {}개", userId, userEmitters.size());
    }
    
    /**
     * 그룹 멤버들에게 히스토리 알림 전송
     */
    public void sendHistoryNotification(UUID groupId, String message, String historyType) {
        try {
            // 그룹 멤버들 조회
            List<UUID> groupMemberIds = groupMembersRepository.findUserIdsByGroupId(groupId);
            
            if (groupMemberIds.isEmpty()) {
                log.warn("⚠️ 그룹 멤버가 없음 - groupId: {}", groupId);
                return;
            }
            
            int sentCount = 0;
            int failedCount = 0;
            
            // 각 멤버에게 알림 전송
            for (UUID memberId : groupMemberIds) {
                SseEmitter emitter = userEmitters.get(memberId);
                if (emitter != null) {
                    try {
                        emitter.send(SseEmitter.event()
                            .name("history-notification")
                            .data(Map.of(
                                "type", historyType,
                                "message", message,
                                "groupId", groupId.toString(),
                                "timestamp", LocalDateTime.now().toString(),
                                "notificationId", UUID.randomUUID().toString()
                            )));
                        sentCount++;
                        
                        log.debug("📤 알림 전송 성공 - userId: {}, message: {}", memberId, message);
                        
                    } catch (Exception e) {
                        failedCount++;
                        log.warn("❌ 알림 전송 실패 - userId: {}, error: {}", memberId, e.getMessage());
                        userEmitters.remove(memberId); // 실패한 연결 제거
                    }
                } else {
                    log.debug("👤 오프라인 사용자 - userId: {}", memberId);
                }
            }
            
            log.info("📊 히스토리 알림 전송 완료 - groupId: {}, 전송: {}개, 실패: {}개, 그룹멤버: {}명, 온라인: {}명", 
                    groupId, sentCount, failedCount, groupMemberIds.size(), userEmitters.size());
                    
        } catch (Exception e) {
            log.error("💥 히스토리 알림 전송 중 오류 - groupId: {}, error: {}", groupId, e.getMessage(), e);
        }
    }
    
    /**
     * 특정 사용자에게 개별 알림 전송
     */
    public void sendNotificationToUser(UUID userId, String message, String type) {
        SseEmitter emitter = userEmitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("user-notification")
                    .data(Map.of(
                        "type", type,
                        "message", message,
                        "timestamp", LocalDateTime.now().toString(),
                        "notificationId", UUID.randomUUID().toString()
                    )));
                
                log.info("📤 개별 알림 전송 성공 - userId: {}, message: {}", userId, message);
                
            } catch (Exception e) {
                log.warn("❌ 개별 알림 전송 실패 - userId: {}, error: {}", userId, e.getMessage());
                userEmitters.remove(userId);
            }
        } else {
            log.debug("👤 오프라인 사용자 - userId: {}", userId);
        }
    }
    
    /**
     * 연결된 사용자 수 조회
     */
    public int getConnectedUserCount() {
        return userEmitters.size();
    }
    
    /**
     * 특정 사용자가 온라인인지 확인
     */
    public boolean isUserOnline(UUID userId) {
        return userEmitters.containsKey(userId);
    }
    
    /**
     * 그룹의 온라인 멤버 수 조회
     */
    public int getOnlineMemberCount(UUID groupId) {
        List<UUID> groupMemberIds = groupMembersRepository.findUserIdsByGroupId(groupId);
        return (int) groupMemberIds.stream()
                .filter(userEmitters::containsKey)
                .count();
    }
}
