package com.example.pay_service.controller;

import com.example.pay_service.domain.PayAccount;
import com.example.pay_service.dto.GroupPayAccountCreateRequest;
import com.example.pay_service.service.PayAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final PayAccountService payAccountService;

    @GetMapping
    public ResponseEntity<List<PayAccount>> getUserAccounts(@AuthenticationPrincipal UUID userId) {
        log.info("사용자 계좌 목록 조회: userId={}", userId);

        List<PayAccount> accounts = payAccountService.getUserAccounts(userId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<PayAccount> getAccount(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UUID userId
    ) {
        log.info("계좌 상세 조회: accountId={}, userId={}", accountId, userId);

        PayAccount account = payAccountService.getAccountByIdAndOwner(accountId, userId);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<PayAccount> getGroupPayAccount(@PathVariable UUID groupId) {
        log.info("그룹 페이 계좌 조회: groupId={}", groupId);

        return payAccountService.getGroupPayAccountByGroupId(groupId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/group-pay/{groupId}")
    public ResponseEntity<PayAccount> createGroupPayAccount(
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupPayAccountCreateRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        log.info("그룹 페이 계좌 생성: userId={}, groupId={}", userId, groupId);

        PayAccount account = payAccountService.createGroupPayAccount(groupId, userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @GetMapping("/group-pay/exists/{groupId}")
    public ResponseEntity<Boolean> hasGroupPayAccount(@PathVariable UUID groupId) {
        log.info("그룹 페이 계좌 존재 확인: groupId={}", groupId);

        boolean exists = payAccountService.hasGroupPayAccount(groupId);
        return ResponseEntity.ok(exists);
    }
}
