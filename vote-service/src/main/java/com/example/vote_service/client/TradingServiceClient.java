package com.example.vote_service.client;

import com.example.vote_service.dto.VoteTradingRequest;
import com.example.vote_service.dto.VoteTradingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "trading-service", url = "${trading-service.url}", configuration = FeignClient.class)
public interface TradingServiceClient {

    @PostMapping("/vote-trading/execute")
    VoteTradingResponse executeVoteBasedTrading(@RequestBody VoteTradingRequest request);
}
