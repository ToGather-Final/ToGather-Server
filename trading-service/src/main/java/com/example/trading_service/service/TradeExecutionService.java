package com.example.trading_service.service;

import com.example.trading_service.domain.*;
import com.example.trading_service.dto.OrderBookResponse;
import com.example.trading_service.event.TradeExecutedEvent;
import com.example.trading_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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
    private final ApplicationEventPublisher eventPublisher;

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
        
        // 그룹 보유량 업데이트는 이벤트로 처리 (순환 참조 방지)
        if (order.getGroupId() != null) {
            log.info("🔍 그룹 거래 - 그룹 보유량 업데이트는 별도 이벤트로 처리됨");
        } else {
            log.info("🔍 개인 거래 - 그룹 보유량 업데이트 건너뜀");
        }
        
        // 거래 체결 이벤트 발행
        eventPublisher.publishEvent(new TradeExecutedEvent(this, order, executionPrice));
        log.info("✅ 거래 체결 이벤트 발행 완료");
        
        
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
                
                // BigDecimal을 사용한 정확한 계산
                BigDecimal currentQuantity = BigDecimal.valueOf(holding.getQuantity());
                BigDecimal currentAvgCost = BigDecimal.valueOf(holding.getAvgCost());
                BigDecimal newQuantity = BigDecimal.valueOf(quantity);
                BigDecimal newPrice = BigDecimal.valueOf(price);
                
                // 새로운 총 수량
                BigDecimal totalQuantity = currentQuantity.add(newQuantity);
                
                // 새로운 평균단가 계산
                BigDecimal currentTotalCost = currentQuantity.multiply(currentAvgCost);
                BigDecimal newTotalCost = newQuantity.multiply(newPrice);
                BigDecimal totalCost = currentTotalCost.add(newTotalCost);
                BigDecimal newAvgCost = totalCost.divide(totalQuantity, 2, RoundingMode.HALF_UP);
                
                // 소수점 6자리로 반올림하여 저장
                holding.setQuantity(totalQuantity.setScale(6, RoundingMode.HALF_UP).floatValue());
                holding.setAvgCost(newAvgCost.setScale(2, RoundingMode.HALF_UP).floatValue());
                holdingCacheRepository.save(holding);
            } else {
                HoldingCache newHolding = new HoldingCache();
                InvestmentAccount account = investmentAccountRepository.findById(accountId)
                        .orElseThrow(() -> new RuntimeException("투자 계좌를 찾을 수 없습니다"));
                Stock stock = stockRepository.findById(stockId)
                        .orElseThrow(() -> new RuntimeException("주식을 찾을 수 없습니다"));
                
                newHolding.setInvestmentAccount(account);
                newHolding.setStock(stock);
                // 소수점 6자리로 반올림
                newHolding.setQuantity(BigDecimal.valueOf(quantity).setScale(6, RoundingMode.HALF_UP).floatValue());
                newHolding.setAvgCost(BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP).floatValue());
                holdingCacheRepository.save(newHolding);
            }
        } else {
            // 매도
            if (existingHolding.isPresent()) {
                HoldingCache holding = existingHolding.get();
                BigDecimal currentQuantity = BigDecimal.valueOf(holding.getQuantity());
                BigDecimal sellQuantity = BigDecimal.valueOf(quantity);
                BigDecimal newQuantity = currentQuantity.subtract(sellQuantity);
                
                if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                    holdingCacheRepository.delete(holding);
                } else {
                    // 소수점 6자리로 반올림
                    holding.setQuantity(newQuantity.setScale(6, RoundingMode.HALF_UP).floatValue());
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
