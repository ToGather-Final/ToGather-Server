package com.example.pay_service.service;

import com.example.pay_service.domain.PaymentSession;
import com.example.pay_service.exception.SessionNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSessionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String SESSION_PREFIX = "ps:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(15);

    public String createSession(UUID groupId, UUID payerUserId, Long amount,
                                String recipientBankCode, String recipientAccountNumber,
                                String recipientName, String recipientBankName) {
        String sessionId = "ps_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);

        PaymentSession session = PaymentSession.create(
                sessionId,
                groupId,
                payerUserId,
                amount,
                recipientBankCode,
                recipientAccountNumber,
                recipientName,
                recipientBankName,
                15  // 15분 TTL
        );

        String key = SESSION_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, session, SESSION_TTL);

        log.info("결제 세션 생성: sessionId={}, groupId={}, amount={}", sessionId, groupId, amount);
        return sessionId;
    }

    public Optional<PaymentSession> getSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        PaymentSession session = (PaymentSession) redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(session);
    }

    public PaymentSession getSessionOrThrow(String sessionId) {
        return getSession(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Payment session not found: " + sessionId));
    }

    public void markAsUsed(String sessionId) {
        Optional<PaymentSession> sessionOpt = getSession(sessionId);
        if (sessionOpt.isPresent()) {
            PaymentSession originalSession = sessionOpt.get();

            // 새로운 객체 생성 (isUsed = true)
            PaymentSession usedSession = PaymentSession.builder()
                    .id(originalSession.getId())
                    .groupId(originalSession.getGroupId())
                    .payerUserId(originalSession.getPayerUserId())
                    .amount(originalSession.getAmount())
                    .recipientBankCode(originalSession.getRecipientBankCode())
                    .recipientAccountNumber(originalSession.getRecipientAccountNumber())
                    .recipientName(originalSession.getRecipientName())
                    .recipientBankName(originalSession.getRecipientBankName())
                    .expiresAt(originalSession.getExpiresAt())
                    .isUsed(true)  // 사용됨으로 표시
                    .createdAt(originalSession.getCreatedAt())
                    .build();

            String key = SESSION_PREFIX + sessionId;
            redisTemplate.opsForValue().set(key, usedSession, SESSION_TTL);

            log.info("결제 세션 사용됨: sessionId={}", sessionId);
        }
    }

    public boolean isSessionValid(String sessionId) {
        Optional<PaymentSession> sessionOpt = getSession(sessionId);
        return sessionOpt.isPresent() && !sessionOpt.get().isUsed();
    }

    // 세션 만료 확인
    public boolean isSessionExpired(String sessionId) {
        Optional<PaymentSession> sessionOpt = getSession(sessionId);
        return sessionOpt.isPresent() && sessionOpt.get().isExpired();
    }

    // 세션 삭제
    public void deleteSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.delete(key);
        log.info("결제 세션 삭제: sessionId={}", sessionId);
    }

    // 만료된 세션 정리 (스케줄러에서 사용)
    public void cleanupExpiredSessions() {
        // Redis에서 만료된 세션들을 찾아서 삭제하는 로직
        // 실제 구현은 Redis의 TTL을 활용하거나 별도 스케줄러에서 처리
        log.info("만료된 결제 세션 정리 완료");
    }
}
