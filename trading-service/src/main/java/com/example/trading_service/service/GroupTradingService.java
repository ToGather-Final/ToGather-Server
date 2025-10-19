package com.example.trading_service.service;

import com.example.trading_service.domain.*;
import com.example.trading_service.dto.BuyRequest;
import com.example.trading_service.dto.GroupHoldingResponse;
import com.example.trading_service.dto.OrderBookResponse;
import com.example.trading_service.dto.SellRequest;
import com.example.trading_service.exception.BusinessException;
import com.example.trading_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupTradingService {

    private final OrderService orderService;
    private final TradeExecutionService tradeExecutionService;
    private final HoldingCacheRepository holdingCacheRepository;
    private final GroupHoldingCacheRepository groupHoldingCacheRepository;
    private final BalanceCacheRepository balanceCacheRepository;
    private final HistoryRepository historyRepository;
    private final InvestmentAccountRepository investmentAccountRepository;
    private final StockRepository stockRepository;
    @Lazy
    private final OrderBookService orderBookService;

    /**
     * 그룹 매수 주문 처리
     * @param groupId 그룹 ID
     * @param stockId 주식 ID
     * @param totalQuantity 총 주문 수량
     * @param price 주문 가격
     * @return 처리된 개인 주문 수
     */
    @Transactional
    public int processGroupBuyOrder(UUID groupId, UUID stockId, int totalQuantity, BigDecimal price) {
        log.info("그룹 매수 주문 시작 - 그룹ID: {}, 주식ID: {}, 수량: {}, 가격: {}", 
                groupId, stockId, totalQuantity, price);

        // 1. 그룹 멤버들의 투자 계좌 조회
        List<InvestmentAccount> groupMembers = getGroupMembers(groupId);
        if (groupMembers.isEmpty()) {
            throw new BusinessException("그룹 멤버를 찾을 수 없습니다.", "GROUP_MEMBERS_NOT_FOUND");
        }

        int memberCount = groupMembers.size();
        
        // 가격과 수량을 그룹 멤버 수로 나누기
        BigDecimal pricePerMember = price.divide(new BigDecimal(memberCount), 2, RoundingMode.HALF_UP);
        BigDecimal quantityPerMember = new BigDecimal(totalQuantity).divide(new BigDecimal(memberCount), 4, RoundingMode.HALF_UP);
        
        log.info("분할 계산 - 원래 가격: {}, 멤버 수: {}, 멤버당 가격: {}, 멤버당 수량: {}", 
                price, memberCount, pricePerMember, quantityPerMember);

        // 2. WebSocket 호가 데이터와 비교하여 체결 가능 여부 확인
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new BusinessException("주식을 찾을 수 없습니다.", "STOCK_NOT_FOUND"));
        
        boolean canExecuteAtRequestedPrice = checkExecutionPossibility(stock.getStockCode(), pricePerMember, "BUY");
        
        if (!canExecuteAtRequestedPrice) {
            log.warn("⚠️ 그룹 매수 주문 - 현재 호가에서 체결 불가능: 종목코드={}, 요청가격={}", 
                    stock.getStockCode(), pricePerMember);
            // 체결 불가능해도 주문은 생성 (지정가 주문으로 대기)
        }

        // 3. 각 멤버별로 개인 주문 생성 및 실행
        List<Order> executedOrders = new ArrayList<>();
        int processedCount = 0;

        for (int i = 0; i < groupMembers.size(); i++) {
            InvestmentAccount memberAccount = groupMembers.get(i);

            try {
                // 개인 매수 주문 생성 (멤버당 가격과 수량 사용)
                BuyRequest buyRequest = new BuyRequest(stockId, null, quantityPerMember.intValue(), pricePerMember, false);

                orderService.buyStock(UUID.fromString(memberAccount.getUserId()), buyRequest);
                
                // 주문 실행은 OrderService 내부에서 처리됨
                // TODO: 실제 주문 객체를 가져와서 executedOrders에 추가
                processedCount++;

                log.info("멤버 {} 매수 완료 - 수량: {}, 가격: {}", 
                        memberAccount.getInvestmentAccountId(), quantityPerMember, pricePerMember);

            } catch (Exception e) {
                log.error("멤버 {} 매수 실패: {}", memberAccount.getInvestmentAccountId(), e.getMessage());
                // 개별 멤버 실패는 로그만 남기고 계속 진행
            }
        }

        // 3. 그룹 보유량 업데이트
        updateGroupHolding(groupId, stockId, totalQuantity, pricePerMember, memberCount);

        // 4. 거래 히스토리 저장
        saveGroupTradingHistory(groupId, stockId, totalQuantity, pricePerMember, "BUY", executedOrders);

        log.info("그룹 매수 주문 완료 - 처리된 주문 수: {}", processedCount);
        return processedCount;
    }

    /**
     * 그룹 매도 주문 처리
     */
    @Transactional
    public int processGroupSellOrder(UUID groupId, UUID stockId, int totalQuantity, BigDecimal price) {
        log.info("그룹 매도 주문 시작 - 그룹ID: {}, 주식ID: {}, 수량: {}, 가격: {}", 
                groupId, stockId, totalQuantity, price);

        // 1. 그룹 보유량 확인
        GroupHoldingCache groupHolding = groupHoldingCacheRepository
                .findByGroupIdAndStock_Id(groupId, stockId)
                .orElseThrow(() -> new BusinessException("그룹 보유 주식을 찾을 수 없습니다.", "GROUP_HOLDING_NOT_FOUND"));

        if (groupHolding.getTotalQuantity() < totalQuantity) {
            throw new BusinessException(
                    String.format("그룹 보유 수량이 부족합니다. 보유: %d, 요청: %d", 
                            groupHolding.getTotalQuantity(), totalQuantity),
                    "INSUFFICIENT_GROUP_HOLDING");
        }

        // 2. 그룹 멤버들의 투자 계좌 조회
        List<InvestmentAccount> groupMembers = getGroupMembers(groupId);
        int memberCount = groupMembers.size();
        
        // 가격과 수량을 그룹 멤버 수로 나누기
        BigDecimal pricePerMember = price.divide(new BigDecimal(memberCount), 2, RoundingMode.HALF_UP);
        BigDecimal quantityPerMember = new BigDecimal(totalQuantity).divide(new BigDecimal(memberCount), 4, RoundingMode.HALF_UP);
        
        log.info("분할 계산 - 원래 가격: {}, 멤버 수: {}, 멤버당 가격: {}, 멤버당 수량: {}", 
                price, memberCount, pricePerMember, quantityPerMember);

        // 3. WebSocket 호가 데이터와 비교하여 체결 가능 여부 확인
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new BusinessException("주식을 찾을 수 없습니다.", "STOCK_NOT_FOUND"));
        
        boolean canExecuteAtRequestedPrice = checkExecutionPossibility(stock.getStockCode(), pricePerMember, "SELL");
        
        if (!canExecuteAtRequestedPrice) {
            log.warn("⚠️ 그룹 매도 주문 - 현재 호가에서 체결 불가능: 종목코드={}, 요청가격={}", 
                    stock.getStockCode(), pricePerMember);
            // 체결 불가능해도 주문은 생성 (지정가 주문으로 대기)
        }

        // 4. 각 멤버별로 개인 매도 주문 생성 및 실행
        List<Order> executedOrders = new ArrayList<>();
        int processedCount = 0;

        for (int i = 0; i < groupMembers.size(); i++) {
            InvestmentAccount memberAccount = groupMembers.get(i);

            try {
                // 개인 매도 주문 생성 (멤버당 가격과 수량 사용)
                SellRequest sellRequest = new SellRequest(stockId, null, quantityPerMember.intValue(), pricePerMember, false);

                orderService.sellStock(UUID.fromString(memberAccount.getUserId()), sellRequest);
                
                // 주문 실행은 OrderService 내부에서 처리됨
                // TODO: 실제 주문 객체를 가져와서 executedOrders에 추가
                processedCount++;

                log.info("멤버 {} 매도 완료 - 수량: {}, 가격: {}", 
                        memberAccount.getInvestmentAccountId(), quantityPerMember, pricePerMember);

            } catch (Exception e) {
                log.error("멤버 {} 매도 실패: {}", memberAccount.getInvestmentAccountId(), e.getMessage());
            }
        }

        // 4. 그룹 보유량 업데이트
        updateGroupHolding(groupId, stockId, -totalQuantity, pricePerMember, memberCount);

        // 5. 거래 히스토리 저장
        saveGroupTradingHistory(groupId, stockId, totalQuantity, pricePerMember, "SELL", executedOrders);

        log.info("그룹 매도 주문 완료 - 처리된 주문 수: {}", processedCount);
        return processedCount;
    }


    /**
     * 그룹 멤버들의 투자 계좌 조회
     */
    private List<InvestmentAccount> getGroupMembers(UUID groupId) {
        try {
            // TODO: 실제 그룹 서비스와 연동하여 그룹 멤버 조회
            // 현재는 임시로 더미 데이터 반환 (그룹 서비스 연동 후 수정 필요)
            
            log.warn("⚠️ 그룹 멤버 조회 - 그룹 서비스 연동 필요: {}", groupId);
            
            // 임시: 그룹 ID를 기반으로 멤버 계좌 생성
            // 실제로는 그룹 서비스에서 멤버 목록을 조회하고, 각 멤버의 투자 계좌를 찾아야 함
            List<InvestmentAccount> members = new ArrayList<>();
            
            // 임시 더미 데이터 (개발/테스트용)
            for (int i = 1; i <= 3; i++) { // 3명으로 줄임 (테스트용)
                InvestmentAccount account = new InvestmentAccount();
                account.setInvestmentAccountId(UUID.randomUUID());
                account.setUserId("group_" + groupId + "_member_" + i);
                members.add(account);
            }
            
            log.info("임시 그룹 멤버 조회 완료 - 그룹ID: {}, 멤버 수: {}", groupId, members.size());
            return members;
            
        } catch (Exception e) {
            log.error("그룹 멤버 조회 실패 - 그룹ID: {} - {}", groupId, e.getMessage());
            throw new BusinessException("그룹 멤버를 조회할 수 없습니다.", "GROUP_MEMBERS_FETCH_FAILED");
        }
    }

    /**
     * WebSocket 호가 데이터와 비교하여 체결 가능 여부 확인
     */
    private boolean checkExecutionPossibility(String stockCode, BigDecimal requestPrice, String orderType) {
        try {
            // WebSocket 호가 데이터 조회
            OrderBookResponse orderBook = orderBookService.getOrderBook(stockCode);
            
            if (orderBook == null || orderBook.getAskPrices().isEmpty() || orderBook.getBidPrices().isEmpty()) {
                log.warn("⚠️ 호가 데이터가 없어 체결 가능 여부 확인 불가: {}", stockCode);
                return false;
            }

            float requestPriceFloat = requestPrice.floatValue();
            
            if ("BUY".equals(orderType)) {
                // 매수 주문: 지정가 >= 최저 매도가
                float lowestAskPrice = orderBook.getAskPrices().get(0).getPrice();
                boolean canExecute = requestPriceFloat >= lowestAskPrice;
                
                log.info("🔍 매수 체결 가능 여부 - 종목코드: {}, 요청가격: {}, 최저매도가: {}, 체결가능: {}", 
                        stockCode, requestPriceFloat, lowestAskPrice, canExecute);
                return canExecute;
                
            } else if ("SELL".equals(orderType)) {
                // 매도 주문: 지정가 <= 최고 매수가
                float highestBidPrice = orderBook.getBidPrices().get(0).getPrice();
                boolean canExecute = requestPriceFloat <= highestBidPrice;
                
                log.info("🔍 매도 체결 가능 여부 - 종목코드: {}, 요청가격: {}, 최고매수가: {}, 체결가능: {}", 
                        stockCode, requestPriceFloat, highestBidPrice, canExecute);
                return canExecute;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("❌ 체결 가능 여부 확인 중 오류 발생 - 종목코드: {} - {}", stockCode, e.getMessage());
            return false;
        }
    }

    /**
     * 그룹 보유량 업데이트
     */
    private void updateGroupHolding(UUID groupId, UUID stockId, int quantityChange, BigDecimal price, int memberCount) {
        Optional<GroupHoldingCache> existingHolding = groupHoldingCacheRepository
                .findByGroupIdAndStock_Id(groupId, stockId);

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new BusinessException("주식을 찾을 수 없습니다.", "STOCK_NOT_FOUND"));

        if (existingHolding.isPresent()) {
            // 기존 보유량 업데이트
            GroupHoldingCache holding = existingHolding.get();
            int newQuantity = holding.getTotalQuantity() + quantityChange;
            
            if (newQuantity <= 0) {
                groupHoldingCacheRepository.delete(holding);
            } else {
                // 평균 매입가 계산
                float newAvgCost = calculateNewAverageCost(
                        holding.getTotalQuantity(), holding.getAvgCost(),
                        quantityChange, price.floatValue());
                
                holding.setTotalQuantity(newQuantity);
                holding.setAvgCost(newAvgCost);
                holding.setMemberCount(memberCount);
                groupHoldingCacheRepository.save(holding);
            }
        } else if (quantityChange > 0) {
            // 새로운 보유량 생성
            GroupHoldingCache newHolding = new GroupHoldingCache();
            newHolding.setGroupId(groupId);
            newHolding.setStock(stock);
            newHolding.setTotalQuantity(quantityChange);
            newHolding.setAvgCost(price.floatValue());
            newHolding.setMemberCount(memberCount);
            groupHoldingCacheRepository.save(newHolding);
        }
    }

    /**
     * 새로운 평균 매입가 계산
     */
    private float calculateNewAverageCost(int currentQuantity, float currentAvgCost, 
                                        int newQuantity, float newPrice) {
        if (currentQuantity <= 0) return newPrice;
        
        long totalCost = (long) currentQuantity * (long) currentAvgCost + 
                        (long) newQuantity * (long) newPrice;
        int totalQuantity = currentQuantity + newQuantity;
        
        return (float) totalCost / totalQuantity;
    }

    /**
     * 그룹 거래 히스토리 저장
     */
    private void saveGroupTradingHistory(UUID groupId, UUID stockId, int quantity, 
                                       BigDecimal price, String transactionType, 
                                       List<Order> executedOrders) {
        for (Order order : executedOrders) {
            History history = new History();
            history.setInvestmentAccount(order.getInvestmentAccount());
            history.setStock(order.getStock());
            history.setTransactionType(History.TransactionType.valueOf(transactionType));
            history.setQuantity(quantity);
            history.setPrice(price);
            history.setTotalAmount(price.multiply(BigDecimal.valueOf(quantity)));
            history.setOrderId(order.getOrderId());
            history.setGroupId(groupId);
            
            historyRepository.save(history);
        }
    }

    /**
     * 그룹 보유종목 조회
     * @param groupId 그룹 ID
     * @return 그룹 보유종목 목록
     */
    @Transactional(readOnly = true)
    public List<GroupHoldingResponse> getGroupHoldings(UUID groupId) {
        log.info("그룹 보유종목 조회 - 그룹ID: {}", groupId);
        
        // 그룹의 보유 수량이 0보다 큰 종목들만 조회
        List<GroupHoldingCache> groupHoldings = groupHoldingCacheRepository
                .findByGroupIdAndTotalQuantityGreaterThan(groupId, 0);
        
        List<GroupHoldingResponse> responses = new ArrayList<>();
        
        for (GroupHoldingCache holding : groupHoldings) {
            Stock stock = holding.getStock();
            
            // 현재가 조회 (OrderBookService 사용)
            OrderBookResponse orderBook = orderBookService.getOrderBook(stock.getStockCode());
            Float currentPrice = orderBook.getCurrentPrice();
            
            // 평가금액 계산
            float evaluatedPrice = currentPrice * holding.getTotalQuantity();
            
            // 평가손익 계산
            float totalCost = holding.getAvgCost() * holding.getTotalQuantity();
            float profit = evaluatedPrice - totalCost;
            
            // 수익률 계산
            float profitRate = totalCost > 0 ? (profit / totalCost) * 100 : 0;
            
            // 전일 대비 변동 정보 (OrderBook에서 가져옴)
            float changeRate = orderBook.getChangeRate() != null ? orderBook.getChangeRate() : 0;
            float changeAmount = orderBook.getChangeAmount() != null ? 
                    orderBook.getChangeAmount() * holding.getTotalQuantity() : 0;
            
            // 변동 방향 (OrderBook에서 가져옴)
            String changeDirection = orderBook.getChangeDirection() != null ? 
                    orderBook.getChangeDirection() : "unchanged";
            
            // 멤버당 평균 보유 수량 계산
            float avgQuantityPerMember = holding.getMemberCount() > 0 ? 
                    (float) holding.getTotalQuantity() / holding.getMemberCount() : 0;
            
            GroupHoldingResponse response = new GroupHoldingResponse(
                    holding.getGroupHoldingId(),
                    holding.getGroupId(),
                    stock.getId(),
                    stock.getStockCode(),
                    stock.getStockName(),
                    stock.getStockImage(),
                    holding.getTotalQuantity(),
                    holding.getAvgCost(),
                    currentPrice,
                    changeAmount,
                    changeRate,
                    profit,
                    evaluatedPrice,
                    profitRate,
                    changeDirection,
                    holding.getMemberCount(),
                    avgQuantityPerMember
            );
            
            responses.add(response);
        }
        
        log.info("그룹 보유종목 조회 완료 - 그룹ID: {}, 보유종목 수: {}", groupId, responses.size());
        return responses;
    }
}
