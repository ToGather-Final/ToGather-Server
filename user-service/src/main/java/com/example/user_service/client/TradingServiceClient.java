// user-service/src/main/java/com/example/user_service/client/TradingServiceClient.java

package com.example.user_service.client;

import com.example.module_common.dto.InvestmentAccountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "trading-service",
        url = "${app.services.trading-service.url:http://localhost:8081}"
)
public interface TradingServiceClient {

    @GetMapping("/trading/internal/accounts/user/{userId}")
    InvestmentAccountDto getAccountByUserId(@PathVariable("userId") UUID userId);
}