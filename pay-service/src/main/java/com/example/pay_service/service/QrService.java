package com.example.pay_service.service;

import com.example.pay_service.domain.PayAccount;
import com.example.pay_service.domain.PaymentSession;
import com.example.pay_service.dto.QrResolveResponse;
import com.example.pay_service.exception.TokenInvalidException;
import com.example.pay_service.repository.PayAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrService {

    private final PayAccountRepository payAccountRepository;
    private final PaymentSessionService paymentSessionService;

    @Value("${app.pay.qr.secret-key}")
    private String qrSecretKey;

    @Transactional(readOnly = true)
    public QrResolveResponse resolve(String sessionToken, Long amount, UUID userId) {
        log.info("QR 해석 시작: sessionToken={}, amount={}, userId={}", sessionToken, amount, userId);

        PaymentSession session = verifySessionToken(sessionToken);

        List<PayAccount> payerAccounts = payAccountRepository.findByOwnerUserIdAndIsActiveTrue(userId);

        QrResolveResponse response = new QrResolveResponse(
                session.getId(),
                new QrResolveResponse.RecipientInfo(
                        session.getRecipientName(),
                        session.getRecipientBankName(),
                        maskAccountNumber(session.getRecipientAccountNumber()),
                        null
                ),
                amount,
                payerAccounts.stream()
                        .map(account -> new QrResolveResponse.PayerAccount(
                                account.getId().toString(),
                                "GROUP_PAY",
                                account.getNickname() != null ? account.getNickname() : getDefaultDisplayName(account),
                                account.getBalance()
                        ))
                        .collect(Collectors.toList()),
                session.getExpiresAt()
        );

        log.info("QR 해석 완료: sessionId={}, recipientName={}", session.getId(), session.getRecipientName());
        return response;
    }

    private PaymentSession verifySessionToken(String sessionToken) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(sessionToken), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\.");

            if (parts.length != 2) {
                throw new TokenInvalidException("Invalid token format");
            }

            String sessionId = parts[0];
            String signature = parts[1];

            String expectedSignature = generateSignature(sessionId);
            if (!signature.equals(expectedSignature)) {
                throw new TokenInvalidException("Invalid token signature");
            }

            return paymentSessionService.getSessionOrThrow(sessionId);
        } catch (Exception e) {
            log.error("QR 토큰 검증 실패: {}", e.getMessage());
            throw new TokenInvalidException("Invalid or expired token");
        }

    }

    private String generateSignature(String sessionId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(qrSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] signature = mac.doFinal(sessionId.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return accountNumber;
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    private String getDefaultDisplayName(PayAccount account) {
        return "그룹 페이 계좌";
    }

    public String generateQrToken(String sessionId) {
        String signature = generateSignature(sessionId);
        String token = sessionId + "." + signature;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    public String extractSessionIdFromToken(String qrToken) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(qrToken), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\.");
            return parts[0];
        } catch (Exception e) {
            throw new TokenInvalidException("Invalid QR token format");
        }
    }

}