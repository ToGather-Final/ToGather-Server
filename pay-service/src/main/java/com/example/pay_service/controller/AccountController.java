package com.example.pay_service.controller;

import com.example.pay_service.domain.PayAccount;
import com.example.pay_service.dto.GroupPayAccountCreateRequest;
import com.example.pay_service.service.PayAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/pay/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "계좌 관리", description = "사용자 계좌 및 그룹 페이 계좌 관리 관련 API")
public class AccountController {

    private final PayAccountService payAccountService;

    @Operation(summary = "사용자 계좌 목록 조회", description = "현재 사용자의 모든 계좌 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "계좌 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<List<PayAccount>> getUserAccounts(@AuthenticationPrincipal UUID userId) {
        log.info("사용자 계좌 목록 조회: userId={}", userId);

        List<PayAccount> accounts = payAccountService.getUserAccounts(userId);
        return ResponseEntity.ok(accounts);
    }

    @Operation(summary = "계좌 상세 조회", description = "특정 계좌의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "계좌 상세 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "계좌 접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음")
    })
    @GetMapping("/{accountId}")
    public ResponseEntity<PayAccount> getAccount(
            @Parameter(description = "계좌 ID", required = true) @PathVariable UUID accountId,
            @AuthenticationPrincipal UUID userId
    ) {
        log.info("계좌 상세 조회: accountId={}, userId={}", accountId, userId);

        PayAccount account = payAccountService.getAccountByIdAndOwner(accountId, userId);
        return ResponseEntity.ok(account);
    }

    @Operation(summary = "그룹 페이 계좌 조회", description = "특정 그룹의 페이 계좌 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "그룹 페이 계좌 조회 성공"),
        @ApiResponse(responseCode = "404", description = "그룹 페이 계좌를 찾을 수 없음")
    })
    @GetMapping("/group/{groupId}")
    public ResponseEntity<PayAccount> getGroupPayAccount(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId) {
        log.info("그룹 페이 계좌 조회: groupId={}", groupId);

        return payAccountService.getGroupPayAccountByGroupId(groupId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "그룹 페이 계좌 생성", description = "특정 그룹을 위한 페이 계좌를 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "그룹 페이 계좌 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "409", description = "이미 그룹 페이 계좌가 존재함")
    })
    @PostMapping("/group-pay/{groupId}")
    public ResponseEntity<PayAccount> createGroupPayAccount(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId,
            @Parameter(description = "그룹 페이 계좌 생성 요청 데이터", required = true) @Valid @RequestBody GroupPayAccountCreateRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        log.info("그룹 페이 계좌 생성: userId={}, groupId={}", userId, groupId);

        PayAccount account = payAccountService.createGroupPayAccount(groupId, userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @Operation(summary = "그룹 페이 계좌 존재 확인", description = "특정 그룹에 페이 계좌가 존재하는지 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "그룹 페이 계좌 존재 여부 확인 성공")
    })
    @GetMapping("/group-pay/exists/{groupId}")
    public ResponseEntity<Boolean> hasGroupPayAccount(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId) {
        log.info("그룹 페이 계좌 존재 확인: groupId={}", groupId);

        boolean exists = payAccountService.hasGroupPayAccount(groupId);
        return ResponseEntity.ok(exists);
    }
}
