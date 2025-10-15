package com.example.trading_service.service;

import com.example.trading_service.domain.*;
import com.example.trading_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TradeExecutionService {

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final BalanceCacheRepository balanceCacheRepository;
    private final HoldingCacheRepository holdingCacheRepository;
    private final InvestmentAccountRepository investmentAccountRepository;
    private final StockRepository stockRepository;

    // 시장가 주문 처리
    public void processMarketOrder(Order order) {
        // 실제 거래소에서는 실시간 가격을 가져와야 하지만, 
        // 여기서는 주문 가격으로 즉시 체결 처리
        executeTrade(order, order.getPrice());
    }

    // 체결 처리
    public void executeTrade(Order order, float executionPrice) {
        // 체결 기록 생성
        Trade trade = new Trade();
        trade.setOrder(order);
        trade.setQuantity(order.getQuantity());
        trade.setPrice(executionPrice);
        tradeRepository.save(trade);

        // 주문 상태 업데이트
        order.setStatus(Order.Status.FILLED);
        orderRepository.save(order);

        // 잔고 및 보유 종목 업데이트
        updateAccountAfterTrade(order, executionPrice);
        
        log.info("거래가 체결되었습니다. 주문ID: {}, 체결가: {}, 수량: {}", 
                order.getOrderId(), executionPrice, order.getQuantity());
    }

    // 거래 후 계좌 업데이트
    private void updateAccountAfterTrade(Order order, float executionPrice) {
        float totalAmount = executionPrice * order.getQuantity();
        
        if (order.getOrderType() == Order.OrderType.BUY) {
            // 매수: 잔고 차감, 보유 종목 추가/업데이트
            updateBalance(order.getInvestmentAccount().getInvestmentAccountId(), -totalAmount);
            updateHolding(order.getInvestmentAccount().getInvestmentAccountId(), order.getStock().getId(), (int) order.getQuantity(), executionPrice, true);
        } else {
            // 매도: 잔고 증가, 보유 종목 차감
            updateBalance(order.getInvestmentAccount().getInvestmentAccountId(), totalAmount);
            updateHolding(order.getInvestmentAccount().getInvestmentAccountId(), order.getStock().getId(), (int) order.getQuantity(), executionPrice, false);
        }
    }

    // 잔고 업데이트
    private void updateBalance(UUID accountId, float amount) {
        BalanceCache balance = balanceCacheRepository.findByInvestmentAccount_InvestmentAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("잔고 정보를 찾을 수 없습니다"));
        
        balance.setBalance(balance.getBalance() + (int) amount);
        balanceCacheRepository.save(balance);
    }

    // 보유 종목 업데이트
    private void updateHolding(UUID accountId, UUID stockId, int quantity, float price, boolean isBuy) {
        Optional<HoldingCache> existingHolding = holdingCacheRepository
                .findByInvestmentAccount_InvestmentAccountIdAndStock_Id(accountId, stockId);
        
        if (isBuy) {
            // 매수
            if (existingHolding.isPresent()) {
                HoldingCache holding = existingHolding.get();
                float newAvgCost = ((holding.getAvgCost() * holding.getQuantity()) + (price * quantity)) 
                        / (holding.getQuantity() + quantity);
                holding.setQuantity(holding.getQuantity() + quantity);
                holding.setAvgCost(newAvgCost);
                holdingCacheRepository.save(holding);
            } else {
                HoldingCache newHolding = new HoldingCache();
                InvestmentAccount account = investmentAccountRepository.findById(accountId)
                        .orElseThrow(() -> new RuntimeException("투자 계좌를 찾을 수 없습니다"));
                Stock stock = stockRepository.findById(stockId)
                        .orElseThrow(() -> new RuntimeException("주식을 찾을 수 없습니다"));
                
                newHolding.setInvestmentAccount(account);
                newHolding.setStock(stock);
                newHolding.setQuantity(quantity);
                newHolding.setAvgCost(price);
                holdingCacheRepository.save(newHolding);
            }
        } else {
            // 매도
            if (existingHolding.isPresent()) {
                HoldingCache holding = existingHolding.get();
                holding.setQuantity(holding.getQuantity() - quantity);
                if (holding.getQuantity() <= 0) {
                    holdingCacheRepository.delete(holding);
                } else {
                    holdingCacheRepository.save(holding);
                }
            }
        }
    }
}
