package com.example.pay_service.controller;

import com.example.pay_service.dto.QrResolveResponse;
import com.example.pay_service.exception.UnauthorizedException;
import com.example.pay_service.security.UserPrincipal;
import com.example.pay_service.service.QrService;
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

    @GetMapping("/resolve")
    public ResponseEntity<QrResolveResponse> resolve(
            @RequestParam("m") String merchantToken,
            @RequestParam(value = "a", required = false) Long amount
    ) {
        UUID userId = UserPrincipal.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        log.info("QR 해석 요청: merchantToken={}, amount={}, userId={}", merchantToken, amount, userId);

        QrResolveResponse response = qrService.resolve(merchantToken, amount, userId);
        return ResponseEntity.ok(response);
    }
}
