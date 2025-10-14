package com.example.pay_service.controller;

import com.example.pay_service.dto.TransferRequest;
import com.example.pay_service.dto.TransferResponse;
import com.example.pay_service.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/recharge")
    public ResponseEntity<TransferResponse> rechargePayMoney(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UUID userId,
            @RequestHeader("X-Group-Id") UUID groupId
    ) {
        log.info("페이머니 충전 요청: amount={}, userId={}, groupId={}", request.amount(), userId, groupId);

        TransferResponse response = transferService.executeTransfer(request, userId, groupId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{transferId}")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable UUID transferId) {
        log.info("송금 조회 요청: transferId={}", transferId);

        TransferResponse response = transferService.getTransfer(transferId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<TransferResponse>> getTransferHistory(
            @RequestParam UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UUID userId
    ) {
        log.info("송금 내역 조회 요청: accountId={}, page={}, size={}, userId={}", accountId, page, size, userId);

        List<TransferResponse> response = transferService.getTransferHistory(accountId, page, size);
        return ResponseEntity.ok(response);
    }
}
