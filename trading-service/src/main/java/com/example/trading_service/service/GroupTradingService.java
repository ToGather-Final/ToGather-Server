package com.example.trading_service.service;

import com.example.trading_service.domain.*;
import com.example.trading_service.dto.BuyRequest;
import com.example.trading_service.dto.SellRequest;
import com.example.trading_service.exception.BusinessException;
import com.example.trading_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        int quantityPerMember = totalQuantity / memberCount;
        int remainingQuantity = totalQuantity % memberCount;

        log.info("그룹 멤버 수: {}, 멤버당 수량: {}, 남은 수량: {}", memberCount, quantityPerMember, remainingQuantity);

        // 2. 각 멤버별로 개인 주문 생성 및 실행
        List<Order> executedOrders = new ArrayList<>();
        int processedCount = 0;

        for (int i = 0; i < groupMembers.size(); i++) {
            InvestmentAccount memberAccount = groupMembers.get(i);
            
            // 마지막 멤버가 남은 수량 처리
            int memberQuantity = quantityPerMember;
            if (i == groupMembers.size() - 1 && remainingQuantity > 0) {
                memberQuantity += remainingQuantity;
            }

            try {
                // 개인 매수 주문 생성
                BuyRequest buyRequest = new BuyRequest(stockId, null, memberQuantity, price, false);

                orderService.buyStock(UUID.fromString(memberAccount.getUserId()), buyRequest);
                
                // 주문 실행은 OrderService 내부에서 처리됨
                // TODO: 실제 주문 객체를 가져와서 executedOrders에 추가
                processedCount++;

                log.info("멤버 {} 매수 완료 - 수량: {}, 가격: {}", 
                        memberAccount.getInvestmentAccountId(), memberQuantity, price);

            } catch (Exception e) {
                log.error("멤버 {} 매수 실패: {}", memberAccount.getInvestmentAccountId(), e.getMessage());
                // 개별 멤버 실패는 로그만 남기고 계속 진행
            }
        }

        // 3. 그룹 보유량 업데이트
        updateGroupHolding(groupId, stockId, totalQuantity, price, memberCount);

        // 4. 거래 히스토리 저장
        saveGroupTradingHistory(groupId, stockId, totalQuantity, price, "BUY", executedOrders);

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
        int quantityPerMember = totalQuantity / memberCount;
        int remainingQuantity = totalQuantity % memberCount;

        // 3. 각 멤버별로 개인 매도 주문 생성 및 실행
        List<Order> executedOrders = new ArrayList<>();
        int processedCount = 0;

        for (int i = 0; i < groupMembers.size(); i++) {
            InvestmentAccount memberAccount = groupMembers.get(i);
            
            int memberQuantity = quantityPerMember;
            if (i == groupMembers.size() - 1 && remainingQuantity > 0) {
                memberQuantity += remainingQuantity;
            }

            try {
                // 개인 매도 주문 생성
                SellRequest sellRequest = new SellRequest(stockId, null, memberQuantity, price, false);

                orderService.sellStock(UUID.fromString(memberAccount.getUserId()), sellRequest);
                
                // 주문 실행은 OrderService 내부에서 처리됨
                // TODO: 실제 주문 객체를 가져와서 executedOrders에 추가
                processedCount++;

                log.info("멤버 {} 매도 완료 - 수량: {}, 가격: {}", 
                        memberAccount.getInvestmentAccountId(), memberQuantity, price);

            } catch (Exception e) {
                log.error("멤버 {} 매도 실패: {}", memberAccount.getInvestmentAccountId(), e.getMessage());
            }
        }

        // 4. 그룹 보유량 업데이트
        updateGroupHolding(groupId, stockId, -totalQuantity, price, memberCount);

        // 5. 거래 히스토리 저장
        saveGroupTradingHistory(groupId, stockId, totalQuantity, price, "SELL", executedOrders);

        log.info("그룹 매도 주문 완료 - 처리된 주문 수: {}", processedCount);
        return processedCount;
    }

    /**
     * 그룹 멤버들의 투자 계좌 조회
     */
    private List<InvestmentAccount> getGroupMembers(UUID groupId) {
        // TODO: 실제 그룹 서비스와 연동하여 그룹 멤버 조회
        // 현재는 임시로 더미 데이터 반환
        List<InvestmentAccount> members = new ArrayList<>();
        
        // 임시: 그룹 ID를 기반으로 멤버 계좌 생성 (실제로는 그룹 서비스에서 조회)
        for (int i = 1; i <= 5; i++) {
            InvestmentAccount account = new InvestmentAccount();
            account.setInvestmentAccountId(UUID.randomUUID());
            account.setUserId("user" + i);
            // account.setAccountName("그룹" + groupId + "_멤버" + i); // InvestmentAccount에 accountName 필드가 없음
            members.add(account);
        }
        
        return members;
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
}
