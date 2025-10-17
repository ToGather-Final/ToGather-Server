package com.example.trading_service.service;

import com.example.module_common.dto.vote.TradingAction;
import com.example.module_common.dto.vote.VoteTradingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteTradingService {

    private final GroupTradingService groupTradingService;

    /**
     * 투표 결과에 따른 그룹 거래 실행
     * @param request 투표 거래 요청
     * @return 처리된 거래 수
     */
    @Transactional
    public int executeVoteBasedTrading(VoteTradingRequest request) {
        log.info("투표 기반 거래 실행 시작 - 투표ID: {}, 그룹ID: {}, 주식ID: {}", 
                request.proposalId(), request.groupId(), request.stockId());

        try {
            // 투표 결과에 따른 거래 실행
            switch (request.tradingAction()) {
                case BUY:
                    return executeVoteBuyTrading(request);
                case SELL:
                    return executeVoteSellTrading(request);
                case HOLD:
                    log.info("투표 결과: 보유 - 거래 없음");
                    return 0;
                default:
                    throw new IllegalArgumentException("지원하지 않는 거래 액션: " + request.tradingAction());
            }
        } catch (Exception e) {
            log.error("투표 기반 거래 실행 실패 - 투표ID: {}", request.proposalId(), e);
            throw e;
        }
    }

    /**
     * 투표 기반 매수 거래 실행
     */
    private int executeVoteBuyTrading(VoteTradingRequest request) {
        log.info("투표 기반 매수 거래 실행 - 수량: {}, 가격: {}", 
                request.quantity(), request.price());

        return groupTradingService.processGroupBuyOrder(
                request.groupId(),
                request.stockId(),
                request.quantity(),
                request.price()
        );
    }

    /**
     * 투표 기반 매도 거래 실행
     */
    private int executeVoteSellTrading(VoteTradingRequest request) {
        log.info("투표 기반 매도 거래 실행 - 수량: {}, 가격: {}", 
                request.quantity(), request.price());

        return groupTradingService.processGroupSellOrder(
                request.groupId(),
                request.stockId(),
                request.quantity(),
                request.price()
        );
    }

    /**
     * 투표 결과 검증
     */
    public boolean validateVoteResult(VoteTradingRequest request) {
        // 투표 결과의 유효성 검증
        if (request.proposalId() == null || request.groupId() == null || request.stockId() == null) {
            log.warn("투표 거래 요청에 필수 필드가 누락됨");
            return false;
        }

        if (request.tradingAction() != TradingAction.HOLD &&
            (request.quantity() == null || request.quantity() <= 0)) {
            log.warn("거래 수량이 유효하지 않음: {}", request.quantity());
            return false;
        }

        if (request.tradingAction() != TradingAction.HOLD &&
            (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0)) {
            log.warn("거래 가격이 유효하지 않음: {}", request.price());
            return false;
        }

        return true;
    }
}




