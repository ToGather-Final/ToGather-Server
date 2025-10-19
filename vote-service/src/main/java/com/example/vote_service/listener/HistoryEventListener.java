package com.example.vote_service.listener;

import com.example.vote_service.event.HistoryCreatedEvent;
import com.example.vote_service.model.History;
import com.example.vote_service.model.HistoryType;
import com.example.vote_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 히스토리 생성 이벤트 리스너
 * - DB에 History가 저장될 때 자동으로 알림 전송
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HistoryEventListener {
    
    private final NotificationService notificationService;
    
    /**
     * History 엔티티가 저장될 때 자동으로 호출
     */
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleHistoryCreated(HistoryCreatedEvent event) {
        History history = event.getHistory();
        
        log.info("🔔 히스토리 생성 감지 - historyId: {}, type: {}, groupId: {}", 
                history.getHistoryId(), history.getHistoryType(), history.getGroupId());
        
        try {
            // 알림 메시지 생성
            String message = createNotificationMessage(history);
            
            // 그룹 멤버들에게 알림 전송
            notificationService.sendHistoryNotification(
                history.getGroupId(),
                message,
                history.getHistoryType().toString()
            );
            
            log.info("✅ 히스토리 알림 전송 완료 - groupId: {}, message: {}", 
                    history.getGroupId(), message);
                    
        } catch (Exception e) {
            log.error("❌ 히스토리 알림 전송 실패 - historyId: {}, error: {}", 
                    history.getHistoryId(), e.getMessage(), e);
        }
    }
    
    /**
     * 히스토리 타입별 알림 메시지 생성
     */
    private String createNotificationMessage(History history) {
        HistoryType type = history.getHistoryType();
        
        switch (type) {
            case VOTE_CREATED:
                return "🗳️ 새로운 투표가 생성되었습니다.";
            case VOTE_APPROVED:
                return "✅ 투표가 가결되었습니다.";
            case VOTE_REJECTED:
                return "❌ 투표가 부결되었습니다.";
            case TRADE_EXECUTED:
                return "💰 거래가 체결되었습니다.";
            case TRADE_FAILED:
                return "⚠️ 거래가 실패했습니다.";
            case CASH_DEPOSIT_COMPLETED:
                return "💳 예수금 충전이 완료되었습니다.";
            case PAY_CHARGE_COMPLETED:
                return "💸 페이 충전이 완료되었습니다.";
            case GOAL_ACHIEVED:
                return "🎯 목표가 달성되었습니다.";
            default:
                return "📢 새로운 소식이 있습니다.";
        }
    }
}
