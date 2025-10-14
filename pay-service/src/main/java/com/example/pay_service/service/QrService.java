package com.example.pay_service.service;

import com.example.pay_service.domain.Account;
import com.example.pay_service.domain.Merchant;
import com.example.pay_service.dto.QrResolveResponse;
import com.example.pay_service.exception.TokenInvalidException;
import com.example.pay_service.repository.AccountRepository;
import com.example.pay_service.repository.MerchantRepository;
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
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrService {

    private final MerchantRepository merchantRepository;
    private final AccountRepository accountRepository;
    private final PaymentSessionService paymentSessionService;

    @Value("${app.pay.qr.secret-key}")
    private String qrSecretKey;

    @Transactional(readOnly = true)
    public QrResolveResponse resolve(String merchantToken, Long amount, UUID userId) {
        log.info("QR 해석 시작: merchantToken={}, amount={}, userId={}", merchantToken, amount, userId);

        Merchant merchant = verifyMerchantToken(merchantToken);

        String sessionId = paymentSessionService.createSession(
                merchant.getId(),
                merchant.getSettlementAccountId(),
                amount
        );

        List<Account> payerAccounts = accountRepository.findByOwnerUserIdAndIsActiveTrue(userId);

        QrResolveResponse response = new QrResolveResponse(
                sessionId,
                new QrResolveResponse.MerchantInfo(
                        merchant.getId().toString(),
                        merchant.getDisplayName(),
                        "ToGather-BANK",
                        maskAccountNumber(merchant.getSettlementAccountId().toString()),
                        merchant.getLogoUrl()
                ),
                amount,
                payerAccounts.stream()
                        .map(account -> new QrResolveResponse.PayerAccount(
                                account.getId().toString(),
                                account.getType(),
                                account.getNickname() != null ? account.getNickname() : getDefaultDisplayName(account),
                                account.getBalance()
                        ))
                        .collect(Collectors.toList()),
                LocalDateTime.now().plusMinutes(15)
        );

        log.info("QR 해석 완료: sessionId={}, merchantId={}", sessionId, merchant.getId());
        return response;
    }

    private Merchant verifyMerchantToken(String merchantToken) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(merchantToken), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\.");

            if (parts.length != 2) {
                throw new TokenInvalidException("Invalid token format");
            }

            String merchantId = parts[0];
            String signature = parts[1];

            String expectedSignature = generateSignature(merchantId);
            if (!signature.equals(expectedSignature)) {
                throw new TokenInvalidException("Invalid token signature");
            }

            UUID merchantUuid = UUID.fromString(merchantId);
            return merchantRepository.findByIdAndIsActiveTrue(merchantUuid)
                    .orElseThrow(() -> new TokenInvalidException("Merchant not found"));
        } catch (Exception e) {
            log.error("QR 토큰 검증 실패: {}", e.getMessage());
            throw new TokenInvalidException("Invalid or expired token");
        }

    }

    private String generateSignature(String merchantId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(qrSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] signature = mac.doFinal(merchantId.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    private String maskAccountNumber(String accountId) {
        if (accountId.length() < 8) {
            return accountId;
        }
        return accountId.substring(0, 3) + "-***-" + accountId.substring(accountId.length() - 4);
    }

    private String getDefaultDisplayName(Account account) {
        return switch (account.getType()) {
            case CMA -> "내 CMA";
            case GROUP_PAY -> "그룹 페이 계좌";
            case MERCHANT -> "가맹점 계좌";
        };
    }
}