package com.example.trading_service.client;

import com.example.trading_service.dto.TransferRequest;
import com.example.trading_service.dto.TransferResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "pay-service", url = "${pay-service.url}")
public interface PayServiceClient {

    @PostMapping("/transfers")
    TransferResponse transferFunds(@RequestBody TransferRequest request);
}
