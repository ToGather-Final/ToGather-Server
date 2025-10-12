package com.example.pay_service.controller;

import com.example.pay_service.domain.Account;
import com.example.pay_service.domain.AccountType;
import com.example.pay_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<Account>> getUserAccounts(@AuthenticationPrincipal UUID userId) {
        log.info("사용자 계좌 목록 조회: userId={}", userId);

        List<Account> accounts = accountService.getUserAccounts(userId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Account>> getUserAccountsByType(
            @PathVariable AccountType type,
            @AuthenticationPrincipal UUID userId
    ) {
        log.info("타입별 계좌 조회: type={}, userId={}", type, userId);

        List<Account> accounts = accountService.getUserAccountsByType(userId, type);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<Account> getAccount(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UUID userId
    ) {
        log.info("계좌 상세 조회: accountId={}, userId={}", accountId, userId);

        Account account = accountService.getAccountByIdAndOwner(accountId, userId);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<Account> getGroupPayAccount(@PathVariable UUID groupId) {
        log.info("그룹 페이 계좌 조회: groupId={}", groupId);

        return accountService.getGroupPayAccountByGroupId(groupId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
