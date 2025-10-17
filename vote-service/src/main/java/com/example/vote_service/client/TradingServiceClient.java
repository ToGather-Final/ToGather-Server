package com.example.vote_service.client;


import com.example.module_common.dto.vote.VoteTradingRequest;
import com.example.module_common.dto.vote.VoteTradingResponse;
import com.example.vote_service.config.FeignConfig;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(name = "trading-service", url = "${trading-service.url}", configuration = FeignConfig.class)

public interface TradingServiceClient {

    @PostMapping("/vote-trading/execute")
    VoteTradingResponse executeVoteBasedTrading(@RequestBody VoteTradingRequest request);
}
