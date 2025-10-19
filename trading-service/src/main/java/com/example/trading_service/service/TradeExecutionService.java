package com.example.trading_service.service;

import com.example.trading_service.domain.*;
import com.example.trading_service.dto.OrderBookResponse;
import com.example.trading_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
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
    @Lazy
    private final OrderBookService orderBookService;
    private final HistoryRepository historyRepository;

    // 시장가 주문 처리
    public void processMarketOrder(Order order) {
        // 실제 거래소에서는 실시간 가격을 가져와야 하지만, 
        // 여기서는 주문 가격으로 즉시 체결 처리
        executeTrade(order, order.getPrice());
    }

    // 지정가 주문 체결 확인 (WebSocket 호가 데이터와 비교)
    public void checkLimitOrderExecution(Order order) {
        try {
            // WebSocket 호가 데이터 조회
            OrderBookResponse orderBook = orderBookService.getOrderBook(order.getStock().getStockCode());
            
            if (orderBook == null || orderBook.getAskPrices().isEmpty() || orderBook.getBidPrices().isEmpty()) {
                log.debug("호가 데이터가 없어 지정가 주문 체결 확인 건너뜀 - 주문ID: {}", order.getOrderId());
                return;
            }

            boolean canExecute = false;
            float executionPrice = 0f;

            if (order.getOrderType() == Order.OrderType.BUY) {
                // 매수 주문: 지정가 >= 최저 매도가
                float lowestAskPrice = orderBook.getAskPrices().get(0).getPrice();
                if (order.getPrice() >= lowestAskPrice) {
                    canExecute = true;
                    executionPrice = lowestAskPrice; // 매도가로 체결
                }
            } else {
                // 매도 주문: 지정가 <= 최고 매수가
                float highestBidPrice = orderBook.getBidPrices().get(0).getPrice();
                if (order.getPrice() <= highestBidPrice) {
                    canExecute = true;
                    executionPrice = highestBidPrice; // 매수가로 체결
                }
            }

            if (canExecute) {
                log.info("지정가 주문 체결 조건 만족 - 주문ID: {}, 지정가: {}, 체결가: {}", 
                        order.getOrderId(), order.getPrice(), executionPrice);
                executeTrade(order, executionPrice);
            } else {
                log.debug("지정가 주문 체결 조건 미만족 - 주문ID: {}, 지정가: {}", 
                        order.getOrderId(), order.getPrice());
            }

        } catch (Exception e) {
            log.error("지정가 주문 체결 확인 중 오류 발생 - 주문ID: {} - {}", 
                    order.getOrderId(), e.getMessage());
        }
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
        
        
        // History 테이블에 거래 체결 히스토리 저장 (일단 주석 처리)
        /*
        try {
            // 개인 거래의 경우 사용자 ID를 기반으로 임시 그룹 ID 생성
            UUID userId = UUID.fromString(order.getInvestmentAccount().getUserId());
            UUID tempGroupId = UUID.nameUUIDFromBytes(("personal_" + userId.toString()).getBytes());
            
            if (tempGroupId != null) {
                String payload = String.format(
                    "{\"side\":\"%s\",\"stockName\":\"%s\",\"shares\":%d,\"unitPrice\":%d,\"accountBalance\":%d}",
                    order.getOrderType().toString(),
                    order.getStock().getStockName(),
                    (int) trade.getQuantity(),
                    (int) trade.getPrice(),
                    getAccountBalance(order.getInvestmentAccount().getInvestmentAccountId())
                );
                
                String title = String.format("%s %d주 %d원 %s 체결",
                    order.getStock().getStockName(),
                    (int) trade.getQuantity(),
                    (int) trade.getPrice(),
                    order.getOrderType() == Order.OrderType.BUY ? "매수" : "매도"
                );
                
                History history = History.create(
                    tempGroupId,
                    HistoryCategory.TRADE,
                    HistoryType.TRADE_EXECUTED,
                    title,
                    payload,
                    (int) trade.getPrice(),
                    (int) trade.getQuantity()
                );
                
                history.setStockId(order.getStock().getId());
                historyRepository.save(history);
                
                log.info("거래 체결 히스토리 저장 완료 - 임시그룹ID: {}, 종목: {}, 수량: {}", 
                        tempGroupId, order.getStock().getStockName(), trade.getQuantity());
            }
        } catch (Exception e) {
            log.error("거래 체결 히스토리 저장 실패 - 주문ID: {} - {}", order.getOrderId(), e.getMessage());
        }
        */
        
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
        BalanceCache balance = balanceCacheRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("잔고 정보를 찾을 수 없습니다"));
        
        balance.setBalance(balance.getBalance() + (int) amount);
        balanceCacheRepository.save(balance);
    }

    // 보유 종목 업데이트
    private void updateHolding(UUID accountId, UUID stockId, int quantity, float price, boolean isBuy) {
        Optional<HoldingCache> existingHolding = holdingCacheRepository
                .findByAccountIdAndStockId(accountId, stockId);
        
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

    /**
     * 계좌 잔액 조회
     */
    private int getAccountBalance(UUID accountId) {
        try {
            return balanceCacheRepository.findByAccountId(accountId)
                    .map(balance -> balance.getBalance())
                    .orElse(0);
        } catch (Exception e) {
            log.error("계좌 잔액 조회 실패 - 계좌ID: {} - {}", accountId, e.getMessage());
            return 0;
        }
    }
}
