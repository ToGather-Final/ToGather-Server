package com.example.pay_service.service;

import com.example.pay_service.client.UserServiceClient;
import com.example.pay_service.domain.PayAccount;
import com.example.pay_service.dto.GroupPayAccountCreateRequest;
import com.example.pay_service.exception.AccountNotOwnedException;
import com.example.pay_service.repository.PayAccountRepository;
import com.example.pay_service.util.PayAccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayAccountService {

    private final PayAccountRepository payAccountRepository;
    private final UserServiceClient userServiceClient;

    @Transactional(readOnly = true)
    public List<PayAccount> getUserAccounts(UUID userId) {
        return payAccountRepository.findByOwnerUserIdAndIsActiveTrue(userId);
    }

    @Transactional(readOnly = true)
    public PayAccount getAccountById(UUID accountId) {
        return payAccountRepository.findByIdAndIsActiveTrue(accountId)
                .orElseThrow(() -> new AccountNotOwnedException("Account not found"));
    }

    @Transactional(readOnly = true)
    public PayAccount getAccountByIdAndOwner(UUID accountId, UUID userId) {
        return payAccountRepository.findByIdAndOwnerUserIdAndIsActiveTrue(accountId, userId)
                .orElseThrow(() -> new AccountNotOwnedException("Account not owned by user"));
    }

    @Transactional(readOnly = true)
    public Optional<PayAccount> getGroupPayAccountByGroupId(UUID groupId) {
        return payAccountRepository.findGroupPayAccountByGroupId(groupId);
    }

    @Transactional(readOnly = true)
    public boolean isAccountOwnedByUser(UUID accountId, UUID userId) {
        return payAccountRepository.existsByIdAndOwnerUserIdAndIsActiveTrue(accountId, userId);
    }

    @Transactional(readOnly = true)
    public boolean hasUserAccount(UUID userId) {
        return payAccountRepository.existsByOwnerUserIdAndIsActiveTrue(userId);
    }

    @Transactional(readOnly = true)
    public boolean hasGroupPayAccount(UUID groupId) {
        return payAccountRepository.existsByGroupIdAndIsActiveTrue(groupId);
    }

    // 추가 유틸리티 메서드들
    @Transactional(readOnly = true)
    public List<PayAccount> getGroupAccounts(UUID groupId) {
        return payAccountRepository.findByGroupIdAndIsActiveTrue(groupId);
    }

    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname, UUID userId) {
        return payAccountRepository.findByNicknameAndOwnerUserIdAndIsActiveTrue(nickname, userId).isEmpty();
    }

    @Transactional
    public PayAccount updateAccountNickname(UUID accountId, UUID userId, String newNickname) {
        PayAccount payAccount = getAccountByIdAndOwner(accountId, userId);

        if (!isNicknameAvailable(newNickname, userId)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // PayAccount는 불변 객체이므로 새로운 객체 생성
        PayAccount updatedAccount = PayAccount.builder()
                .id(payAccount.getId())
                .ownerUserId(payAccount.getOwnerUserId())
                .balance(payAccount.getBalance())
                .nickname(newNickname)
                .isActive(payAccount.getIsActive())
                .groupId(payAccount.getGroupId())
                .build();

        return payAccountRepository.save(updatedAccount);
    }

    @Transactional
    public PayAccount createGroupPayAccount(UUID groupId, UUID userId, GroupPayAccountCreateRequest request) {
        if (hasGroupPayAccount(groupId)) {
            throw new IllegalArgumentException("이미 그룹 페이 계좌가 존재합니다.");
        }

        if (!request.agreeToTerms()) {
            throw new IllegalArgumentException("개인정보 처리 동의가 필요합니다.");
        }

        String accountNumber = generateUniqueAccountNumber();

        String groupNickname = "그룹 페이 계좌";

        PayAccount payAccount = PayAccount.builder()
                .ownerUserId(userId)
                .balance(0L)
                .nickname(groupNickname)
                .isActive(true)
                .groupId(groupId)
                .accountNumber(accountNumber)
                .build();

        return payAccountRepository.save(payAccount);
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            accountNumber = PayAccountNumberGenerator.generatePayAccountNumber();
        } while (payAccountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }

    @Transactional(readOnly = true)
    public boolean isGroupLeader(UUID groupId, UUID userId) {
        return payAccountRepository.existsByGroupIdAndOwnerUserIdAndIsActiveTrue(groupId, userId);
    }

    @Transactional(readOnly = true)
    public boolean isGroupMember(UUID groupId, UUID userId) {
        try {
            Boolean isMember = userServiceClient.isGroupMember(groupId, userId);
            return isMember != null && isMember;
        } catch (Exception e) {
            log.warn("그룹원 확인 실패: groupId={}, userId={}, error={}", groupId, userId, e.getMessage());
            return false; // 실패 시 false 반환
        }
    }
}