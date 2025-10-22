package com.example.pay_service.controller;

import com.example.pay_service.dto.QrResolveResponse;
import com.example.pay_service.exception.UnauthorizedException;
import com.example.pay_service.security.UserPrincipal;
import com.example.pay_service.service.QrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/qr")
@RequiredArgsConstructor
@Slf4j
public class QrController {

    private final QrService qrService;

    @Operation(summary = "QR 코드 해석", description = "QR 코드를 해석하여 결제 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "QR 코드 해석 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 QR 코드"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/resolve")
    public ResponseEntity<QrResolveResponse> resolve(
            @RequestParam("m") String merchantToken,
            @RequestParam(value = "a", required = false) Long amount,
            @AuthenticationPrincipal UUID userId
    ) {
        log.info("QR 해석 요청: merchantToken={}, amount={}, userId={}", merchantToken, amount, userId);

        QrResolveResponse response = qrService.resolve(merchantToken, amount, userId);
        return ResponseEntity.ok(response);
    }
}