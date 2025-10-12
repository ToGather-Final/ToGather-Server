package com.example.pay_service.service;

import com.example.pay_service.domain.Account;
import com.example.pay_service.domain.AccountType;
import com.example.pay_service.exception.AccountNotOwnedException;
import com.example.pay_service.repository.AccountRepository;
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
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<Account> getUserAccounts(UUID userId) {
        return accountRepository.findByOwnerUserIdAndIsActiveTrue(userId);
    }

    @Transactional(readOnly = true)
    public List<Account> getUserAccountsByType(UUID userId, AccountType type) {
        return accountRepository.findByOwnerUserIdAndTypeAndIsActiveTrue(userId, type);
    }

    @Transactional(readOnly = true)
    public Account getAccountById(UUID accountId) {
        return accountRepository.findByIdAndIsActiveTrue(accountId)
                .orElseThrow(() -> new AccountNotOwnedException("Account not found"));
    }

    @Transactional(readOnly = true)
    public Account getAccountByIdAndOwner(UUID accountId, UUID userId) {
        return accountRepository.findByIdAndOwnerUserIdAndIsActiveTrue(accountId, userId)
                .orElseThrow(() -> new AccountNotOwnedException("Account not owned by user"));
    }

    @Transactional(readOnly = true)
    public Optional<Account> getGroupPayAccountByGroupId(UUID groupId) {
        return accountRepository.findGroupPayAccountByGroupId(groupId);
    }

    @Transactional(readOnly = true)
    public boolean isAccountOwnedByUser(UUID accountId, UUID userId) {
        return accountRepository.existsByIdAndOwnerUserIdAndIsActiveTrue(accountId, userId);
    }

    @Transactional(readOnly = true)
    public boolean hasUserAccountType(UUID userId, AccountType type) {
        return accountRepository.existsByOwnerUserIdAndTypeAndIsActiveTrue(userId, type);
    }
}
