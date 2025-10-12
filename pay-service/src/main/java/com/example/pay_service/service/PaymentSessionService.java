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

    public String createSession(UUID merchantId, UUID merchantAccountId, Long suggestedAmount) {
        String sessionId = "ps_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);

        PaymentSession session = PaymentSession.builder()
                .id(sessionId)
                .merchantId(merchantId)
                .merchantAccountId(merchantAccountId)
                .suggestedAmount(suggestedAmount)
                .used(false)
                .build();

        String key = SESSION_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, session, SESSION_TTL);

        log.info("결제 세션 생성: sessionId={}, merchantId={}", sessionId, merchantId);
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
            PaymentSession session = sessionOpt.get();
            session.setUsed(true);

            String key = SESSION_PREFIX + sessionId;
            redisTemplate.opsForValue().set(key, session, SESSION_TTL);

            log.info("결제 세션 사용됨: sessionId={}", sessionId);
        }
    }

    public boolean isSessionValid(String sessionId) {
        Optional<PaymentSession> sessionOpt = getSession(sessionId);
        return sessionOpt.isPresent() && !sessionOpt.get().isUsed();
    }
}
