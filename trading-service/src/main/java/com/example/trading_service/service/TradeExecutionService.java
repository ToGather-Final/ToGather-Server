package com.example.trading_service.service;

import com.example.trading_service.domain.*;
import com.example.trading_service.dto.OrderBookResponse;
import com.example.trading_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
                    log.info("💰 지정가 주문 체결 가능 - 주문ID: {}, 지정가: {}원, 최저매도가: {}원, 체결가: {}원", 
                            order.getOrderId(), order.getPrice(), lowestAskPrice, executionPrice);
                } else {
                    log.debug("⏳ 지정가 매수 대기 - 주문ID: {}, 지정가: {}원 < 최저매도가: {}원", 
                            order.getOrderId(), order.getPrice(), lowestAskPrice);
                }
            } else {
                // 매도 주문: 지정가 <= 최고 매수가
                float highestBidPrice = orderBook.getBidPrices().get(0).getPrice();
                if (order.getPrice() <= highestBidPrice) {
                    canExecute = true;
                    executionPrice = highestBidPrice; // 매수가로 체결
                    log.info("💰 지정가 주문 체결 가능 - 주문ID: {}, 지정가: {}원, 최고매수가: {}원, 체결가: {}원", 
                            order.getOrderId(), order.getPrice(), highestBidPrice, executionPrice);
                } else {
                    log.debug("⏳ 지정가 매도 대기 - 주문ID: {}, 지정가: {}원 > 최고매수가: {}원", 
                            order.getOrderId(), order.getPrice(), highestBidPrice);
                }
            }

            if (canExecute) {
                executeTrade(order, executionPrice);
            }

        } catch (Exception e) {
            log.error("지정가 주문 체결 확인 중 오류 발생 - 주문ID: {} - {}", 
                    order.getOrderId(), e.getMessage());
        }
    }

    // 체결 처리
    public void executeTrade(Order order, float executionPrice) {
        log.info("🚀 거래 체결 시작 - 주문ID: {}, 체결가: {}, 수량: {}", 
                order.getOrderId(), executionPrice, order.getQuantity());
        
        // 체결 기록 생성
        Trade trade = new Trade();
        trade.setOrder(order);
        trade.setQuantity(order.getQuantity());
        trade.setPrice(executionPrice);
        tradeRepository.save(trade);
        log.info("✅ Trade 엔티티 저장 완료 - tradeId: {}", trade.getTradeId());

        // 주문 상태 업데이트
        order.setStatus(Order.Status.FILLED);
        orderRepository.save(order);
        log.info("✅ 주문 상태 업데이트 완료 - status: FILLED");

        // 잔고 및 보유 종목 업데이트
        updateAccountAfterTrade(order, executionPrice);
        log.info("✅ 계좌 업데이트 완료");
        
        
        // 🔥 개인 거래 히스토리는 저장하지 않음 (그룹 거래에서만 히스토리 저장)
        log.info("🔍 개인 거래 체결 완료 - 히스토리는 그룹 거래에서만 저장됨");
        
        log.info("거래가 체결되었습니다. 주문ID: {}, 체결가: {}, 수량: {}", 
                order.getOrderId(), executionPrice, order.getQuantity());
    }

    // 거래 후 계좌 업데이트
    private void updateAccountAfterTrade(Order order, float executionPrice) {
        float totalAmount = executionPrice * order.getQuantity();
        
        if (order.getOrderType() == Order.OrderType.BUY) {
            // 매수: 잔고 차감, 보유 종목 추가/업데이트
            updateBalance(order.getInvestmentAccount().getInvestmentAccountId(), -totalAmount);
            updateHolding(order.getInvestmentAccount().getInvestmentAccountId(), order.getStock().getId(), order.getQuantity(), executionPrice, true);
        } else {
            // 매도: 잔고 증가, 보유 종목 차감
            updateBalance(order.getInvestmentAccount().getInvestmentAccountId(), totalAmount);
            updateHolding(order.getInvestmentAccount().getInvestmentAccountId(), order.getStock().getId(), order.getQuantity(), executionPrice, false);
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
    private void updateHolding(UUID accountId, UUID stockId, float quantity, float price, boolean isBuy) {
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

    /**
     * 그룹 총 예수금 잔액 조회
     * - 투표를 통해 체결된 거래의 경우 그룹 전체 잔액을 히스토리에 포함
     */
    private int getGroupTotalBalance(UUID groupId) {
        try {
            // 개인 거래의 경우 해당 사용자의 잔액만 반환
            // (임시 그룹 ID는 "personal_" + userId 형태)
            String groupIdStr = groupId.toString();
            if (groupIdStr.startsWith("personal_")) {
                // 개인 거래의 경우 해당 계좌의 잔액만 반환
                // TODO: 실제 사용자 ID를 추출하여 해당 사용자의 잔액 조회
                log.info("개인 거래 그룹 잔액 조회 - groupId: {} (임시로 0 반환)", groupId);
                return 0;
            }
            
            // 실제 그룹 거래의 경우 0 반환 (GroupTradingService에서 처리)
            log.info("실제 그룹 거래 잔액 조회 - groupId: {} (임시로 0 반환)", groupId);
            return 0;
            
        } catch (Exception e) {
            log.error("그룹 총 잔액 조회 실패 - groupId: {} - {}", groupId, e.getMessage());
            return 0;
        }
    }
}
