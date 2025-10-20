package com.example.pay_service.client;

import com.example.module_common.dto.InvestmentAccountDto;
import com.example.module_common.dto.TransferToPayResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "trading-service", url = "${app.services.trading-service.url:http://localhost:8081}")
public interface TradingServiceClient {

    @PostMapping("/trading/internal/transfer-to-pay")
    TransferToPayResponse transferToPay(
            @RequestParam("userId") UUID userId,
            @RequestParam("amount") Long amount,
            @RequestParam("transferId") UUID transferId
    );

    @GetMapping("/trading/internal/accounts/user/{userId}")
    InvestmentAccountDto getAccountByUserId(@PathVariable("userId") UUID userId);
}
