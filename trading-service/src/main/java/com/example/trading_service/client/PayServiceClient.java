package com.example.trading_service.client;

import com.example.module_common.dto.pay.PayRechargeRequest;
import com.example.module_common.dto.pay.PayRechargeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "pay-service", url = "${pay-service.url}")
public interface PayServiceClient {

    @PostMapping("/transfers/recharge")
    PayRechargeResponse rechargePayMoney(
            @RequestBody PayRechargeRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Group-Id") UUID groupId
    );
}
