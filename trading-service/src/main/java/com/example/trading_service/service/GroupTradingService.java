package com.example.trading_service.service;

import com.example.module_common.dto.InvestmentAccountDto;
import com.example.trading_service.client.UserServiceClient;
import com.example.trading_service.domain.*;
import com.example.trading_service.dto.BuyRequest;
import com.example.trading_service.dto.GroupHoldingResponse;
import com.example.trading_service.dto.HoldingResponse;
import com.example.trading_service.dto.PortfolioSummaryResponse;
import com.example.trading_service.dto.OrderBookResponse;
import com.example.trading_service.dto.SellRequest;
import com.example.trading_service.exception.BusinessException;
import com.example.trading_service.repository.*;
import com.example.trading_service.event.TradeExecutedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupTradingService {

    private final OrderService orderService;
    private final TradeExecutionService tradeExecutionService;
    private final HoldingCacheRepository holdingCacheRepository;
    private final GroupHoldingCacheRepository groupHoldingCacheRepository;
    private final BalanceCacheRepository balanceCacheRepository;
    // private final HistoryRepository historyRepository; // 히스토리 기능 주석
    private final InvestmentAccountRepository investmentAccountRepository;
    private final StockRepository stockRepository;
    @Lazy
    private final OrderBookService orderBookService;
    private final PortfolioCalculationService portfolioCalculationService;
    private final UserServiceClient userServiceClient;
    private final HistoryRepository historyRepository;

    /**
     * 그룹 매수 주문 처리 (그룹 분할 매매)
     * @param groupId 그룹 ID
     * @param stockId 주식 ID
     * @param totalQuantity 그룹이 함께 살 총 수량 (예: 1주)
     * @param pricePerShare 주식 1주당 가격 (예: 114,700원)
     * @return 처리된 개인 주문 수
     */
    @Transactional
    public int processGroupBuyOrder(UUID groupId, UUID stockId, float totalQuantity, BigDecimal pricePerShare) {
        log.info("그룹 매수 주문 시작 - 그룹ID: {}, 주식ID: {}, 수량: {}주, 주당가격: {}원", 
                groupId, stockId, totalQuantity, pricePerShare);

        // 1. 종목 정보 조회
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new BusinessException("주식을 찾을 수 없습니다.", "STOCK_NOT_FOUND"));

        // 2. 그룹 멤버들의 투자 계좌 조회
        List<InvestmentAccount> groupMembers = getGroupMembers(groupId);
        if (groupMembers.isEmpty()) {
            throw new BusinessException("그룹 멤버를 찾을 수 없습니다.", "GROUP_MEMBERS_NOT_FOUND");
        }

        int memberCount = groupMembers.size();
        
        // 3. 총 투자 금액 계산
        BigDecimal totalInvestment = pricePerShare.multiply(new BigDecimal(totalQuantity));
        
        // 4. 멤버당 부담금 계산 (총 투자 금액 ÷ 멤버 수)
        // HALF_UP: 반올림 (0.5 이상이면 올림, 미만이면 내림)
        BigDecimal costPerMember = totalInvestment.divide(new BigDecimal(memberCount), 2, RoundingMode.HALF_UP);
        
        // 5. 멤버당 수량 계산 (총 수량 ÷ 멤버 수)
        // 소수점 8자리까지 정밀하게 계산 (반올림)
        BigDecimal quantityPerMember = new BigDecimal(String.valueOf(totalQuantity))
                .divide(new BigDecimal(memberCount), 8, RoundingMode.HALF_UP);
        
        log.info("👥 그룹 분할 매매 - 멤버수: {}, 총 투자: {}원, 멤버당 부담금: {}원, 멤버당 수량: {}주", 
                memberCount, totalInvestment, costPerMember, quantityPerMember);

        // 6. 현재 호가 조회 (참고용)
        OrderBookResponse orderBook = orderBookService.getOrderBook(stock.getStockCode());
        if (orderBook != null && !orderBook.getAskPrices().isEmpty()) {
            float currentMarketPrice = orderBook.getAskPrices().get(0).getPrice();
            log.info("💰 현재 시장 호가 - 종목: {}, 매도1호가: {}원 (요청가격: {}원)", 
                    stock.getStockName(), currentMarketPrice, pricePerShare);
        }

        // 7. 각 멤버별로 개인 주문 생성 및 실행
        List<Order> executedOrders = new ArrayList<>();
        List<Exception> failedOrders = new ArrayList<>();
        int processedCount = 0;

        for (InvestmentAccount memberAccount : groupMembers) {
            try {
                // 개인 매수 주문 생성 (요청 가격으로 지정가 주문)
                BuyRequest buyRequest = new BuyRequest(
                    stockId, 
                    null, 
                    quantityPerMember.floatValue(), // 소수점 수량 지원
                    pricePerShare, 
                    false // 지정가 주문
                );

                Order createdOrder = orderService.buyStock(memberAccount.getUserId(), buyRequest, groupId);
                executedOrders.add(createdOrder);
                processedCount++;

                log.info("✅ 멤버 {} 매수 주문 생성 - 수량: {}주, 주당가격: {}원, 부담금: {}원", 
                        memberAccount.getInvestmentAccountId(), quantityPerMember, pricePerShare, costPerMember);

            } catch (Exception e) {
                log.error("❌ 멤버 {} 매수 실패: {}", memberAccount.getInvestmentAccountId(), e.getMessage());
                failedOrders.add(e);
            }
        }

        // 7-1. 그룹 거래 실패 시 하나의 히스토리만 저장
        if (processedCount == 0 && !failedOrders.isEmpty()) {
            // 모든 멤버의 거래가 실패한 경우
            Exception firstError = failedOrders.get(0);
            String reason = firstError.getMessage();
            if (reason.contains("잔고")) {
                reason = "그룹 계좌에 잔액이 부족합니다.";
            } else if (reason.contains("수량")) {
                reason = "주식 보유 수량이 부족합니다.";
            } else {
                reason = "거래가 실패했습니다.";
            }
            
            log.warn("⚠️ 그룹 매수 거래 실패 - 그룹: {}, 종목: {}, 사유: {}", groupId, stock.getStockName(), reason);
            orderService.saveTradeFailedHistory(groupMembers.get(0).getUserId(), stock, 
                new BuyRequest(stockId, null, totalQuantity, pricePerShare, false), 
                "BUY", reason, groupId);
            return 0;
        }


        log.info("그룹 매수 주문 완료 - 처리된 주문 수: {}", processedCount);
        
        // 🔥 그룹 거래 히스토리 저장
        if (processedCount > 0) {
            log.info("🔍 그룹 거래 히스토리 저장 호출 시작 - 그룹ID: {}, 주식ID: {}, 수량: {}", groupId, stockId, totalQuantity);
            saveGroupTradingHistory(groupId, stockId, totalQuantity, pricePerShare, "BUY", executedOrders);
            log.info("🔍 그룹 거래 히스토리 저장 호출 완료");
        } else {
            log.warn("⚠️ 그룹 거래 히스토리 저장 건너뜀 - 처리된 주문 수: 0");
        }
        
        return processedCount;
    }

    /**
     * 그룹 매도 주문 처리
     * @param groupId 그룹 ID
     * @param stockId 주식 ID
     * @param totalQuantity 총 매도 수량
     * @param price 주문 가격
     * @return 처리된 개인 주문 수
     */
    @Transactional
    public int processGroupSellOrder(UUID groupId, UUID stockId, float totalQuantity, BigDecimal price) {
        log.info("그룹 매도 주문 시작 - 그룹ID: {}, 주식ID: {}, 수량: {}주, 주당가격: {}원", 
                groupId, stockId, totalQuantity, price);

        // 1. 종목 정보 조회
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new BusinessException("주식을 찾을 수 없습니다.", "STOCK_NOT_FOUND"));

        // 2. 그룹 보유량 확인
        GroupHoldingCache groupHolding = groupHoldingCacheRepository
                .findByGroupIdAndStock_Id(groupId, stockId)
                .orElseThrow(() -> new BusinessException("그룹 보유 주식을 찾을 수 없습니다.", "GROUP_HOLDING_NOT_FOUND"));

        if (groupHolding.getTotalQuantity() < totalQuantity) {
            throw new BusinessException(
                    String.format("그룹 보유 수량이 부족합니다. 보유: %.2f, 요청: %.2f", 
                            groupHolding.getTotalQuantity(), totalQuantity),
                    "INSUFFICIENT_GROUP_HOLDING");
        }

        // 3. 그룹 멤버들의 투자 계좌 조회
        List<InvestmentAccount> groupMembers = getGroupMembers(groupId);
        int memberCount = groupMembers.size();
        
        // 4. 총 매도 금액 계산
        BigDecimal totalRevenue = price.multiply(BigDecimal.valueOf(totalQuantity));
        
        // 5. 멤버당 수령액 계산 (총 매도 금액 ÷ 멤버 수)
        // HALF_UP: 반올림 (0.5 이상이면 올림, 미만이면 내림)
        BigDecimal revenuePerMember = totalRevenue.divide(new BigDecimal(memberCount), 2, RoundingMode.HALF_UP);
        
        // 6. 멤버당 수량 계산 (총 수량 ÷ 멤버 수)
        // 소수점 8자리까지 정밀하게 계산 (반올림)
        BigDecimal quantityPerMember = new BigDecimal(String.valueOf(totalQuantity))
                .divide(new BigDecimal(memberCount), 8, RoundingMode.HALF_UP);
        
        log.info("👥 그룹 분할 매도 - 멤버수: {}, 총 매도금액: {}원, 멤버당 수령액: {}원, 멤버당 수량: {}주", 
                memberCount, totalRevenue, revenuePerMember, quantityPerMember);

        // 7. 현재 호가 조회 (참고용)
        OrderBookResponse orderBook = orderBookService.getOrderBook(stock.getStockCode());
        if (orderBook != null && !orderBook.getBidPrices().isEmpty()) {
            float currentMarketPrice = orderBook.getBidPrices().get(0).getPrice();
            log.info("💰 현재 시장 호가 - 종목: {}, 매수1호가: {}원 (요청가격: {}원)", 
                    stock.getStockName(), currentMarketPrice, price);
        }

        // 8. 각 멤버별로 개인 매도 주문 생성 및 실행
        List<Order> executedOrders = new ArrayList<>();
        List<Exception> failedOrders = new ArrayList<>();
        int processedCount = 0;

        for (InvestmentAccount memberAccount : groupMembers) {
            try {
                // 개인 매도 주문 생성 (요청 가격으로 지정가 주문)
                SellRequest sellRequest = new SellRequest(
                    stockId, 
                    null, 
                    quantityPerMember.floatValue(), // 소수점 수량 지원
                    price, 
                    false // 지정가 주문
                );

                Order createdOrder = orderService.sellStock(memberAccount.getUserId(), sellRequest, groupId);
                executedOrders.add(createdOrder);
                processedCount++;

                log.info("✅ 멤버 {} 매도 주문 생성 - 수량: {}주, 주당가격: {}원, 수령액: {}원", 
                        memberAccount.getInvestmentAccountId(), quantityPerMember, price, revenuePerMember);

            } catch (Exception e) {
                log.error("❌ 멤버 {} 매도 실패: {}", memberAccount.getInvestmentAccountId(), e.getMessage());
                failedOrders.add(e);
            }
        }

        // 8-1. 그룹 거래 실패 시 하나의 히스토리만 저장
        if (processedCount == 0 && !failedOrders.isEmpty()) {
            // 모든 멤버의 거래가 실패한 경우
            Exception firstError = failedOrders.get(0);
            String reason = firstError.getMessage();
            if (reason.contains("잔고")) {
                reason = "그룹 계좌에 잔액이 부족합니다.";
            } else if (reason.contains("수량")) {
                reason = "주식 보유 수량이 부족합니다.";
            } else {
                reason = "거래가 실패했습니다.";
            }
            
            log.warn("⚠️ 그룹 매도 거래 실패 - 그룹: {}, 종목: {}, 사유: {}", groupId, stock.getStockName(), reason);
            orderService.saveTradeFailedHistory(groupMembers.get(0).getUserId(), stock, 
                new SellRequest(stockId, null, totalQuantity, price, false), 
                "SELL", reason, groupId);
            return 0;
        }


        log.info("그룹 매도 주문 완료 - 처리된 주문 수: {}", processedCount);
        
        // 🔥 그룹 거래 히스토리 저장
        if (processedCount > 0) {
            saveGroupTradingHistory(groupId, stockId, totalQuantity, price, "SELL", executedOrders);
        }
        
        return processedCount;
    }


    /**
     * 거래 체결 이벤트 리스너 - 그룹 보유량 업데이트
     */
    @EventListener
    @Transactional
    public void handleTradeExecutedEvent(TradeExecutedEvent event) {
        try {
            Order order = event.getOrder();
            float executionPrice = event.getExecutionPrice();
            UUID groupId = event.getGroupId();
            
            if (groupId != null) {
                log.info("🔍 그룹 거래 체결 이벤트 처리 - 그룹ID: {}, 주문ID: {}", groupId, order.getOrderId());
                
                UUID stockId = order.getStock().getId();
                float quantity = order.getQuantity();
                
                // 매수/매도에 따라 수량 조정
                float quantityChange = (order.getOrderType() == Order.OrderType.BUY) ? quantity : -quantity;
                
                // 그룹 멤버 수 조회
                int memberCount = getGroupMemberCount(groupId);
                
                // 그룹 보유량 업데이트
                updateGroupHoldingAfterTrade(
                    groupId, stockId, quantityChange, executionPrice, memberCount
                );
                
                log.info("✅ 그룹 보유량 업데이트 완료 - 그룹ID: {}, 종목ID: {}, 수량변화: {}", 
                    groupId, stockId, quantityChange);
            } else {
                log.debug("🔍 개인 거래 - 그룹 보유량 업데이트 건너뜀");
            }
            
        } catch (Exception e) {
            log.error("❌ 그룹 거래 체결 이벤트 처리 실패 - 주문ID: {} - {}", 
                event.getOrder().getOrderId(), e.getMessage());
        }
    }

    /**
     * 그룹 멤버 수 조회
     */
    public int getGroupMemberCount(UUID groupId) {
        try {
            List<InvestmentAccountDto> memberDtos = userServiceClient.getGroupMemberAccounts(groupId);
            return memberDtos.size();
        } catch (Exception e) {
            log.error("그룹 멤버 수 조회 실패 - 그룹ID: {} - {}", groupId, e.getMessage());
            return 0;
        }
    }

    /**
     * 그룹 멤버들의 투자 계좌 조회
     */
    public List<InvestmentAccount> getGroupMembers(UUID groupId) {
        try {
            List<InvestmentAccountDto> memberDtos = userServiceClient.getGroupMemberAccounts(groupId);
            
            if (memberDtos.isEmpty()) {
                log.warn("그룹에 실제 멤버가 없습니다 - groupId: {}", groupId);
                throw new BusinessException("그룹에 멤버가 없습니다.");
            }
            
            List<InvestmentAccount> members = new ArrayList<>();
            for (InvestmentAccountDto dto : memberDtos) {
                InvestmentAccount account = investmentAccountRepository.findById(dto.getInvestmentAccountId())
                        .orElseThrow(() -> new BusinessException("투자 계좌를 찾을 수 없습니다."));
                members.add(account);
            }
            
            log.info("실제 그룹 멤버 조회 완료 - 그룹ID: {}, 멤버 수: {}", groupId, members.size());
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
     * 그룹 보유량 업데이트 (외부 호출용 - public)
     */
    @Transactional
    public void updateGroupHoldingAfterTrade(UUID groupId, UUID stockId, float totalQuantity, float price, int memberCount) {
        updateGroupHolding(groupId, stockId, totalQuantity, BigDecimal.valueOf(price), memberCount);
    }
    
    /**
     * 그룹 보유량 업데이트 (내부용 - private)
     */
    private void updateGroupHolding(UUID groupId, UUID stockId, float quantityChange, BigDecimal price, int memberCount) {
        Optional<GroupHoldingCache> existingHolding = groupHoldingCacheRepository
                .findByGroupIdAndStock_Id(groupId, stockId);

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new BusinessException("주식을 찾을 수 없습니다.", "STOCK_NOT_FOUND"));

        if (existingHolding.isPresent()) {
            // 기존 보유량 업데이트
            GroupHoldingCache holding = existingHolding.get();
            float newQuantity = holding.getTotalQuantity() + quantityChange;
            
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
                
                // 평가금액과 손익 계산
                try {
                    OrderBookResponse orderBook = orderBookService.getOrderBook(stock.getStockCode());
                    float currentPrice = orderBook.getCurrentPrice();
                    holding.setEvaluatedPrice(currentPrice * newQuantity);
                    holding.setProfit((currentPrice - newAvgCost) * newQuantity);
                } catch (Exception e) {
                    log.warn("주가 조회 실패, 평균 매입가로 대체 - 종목: {}", stock.getStockCode());
                    holding.setEvaluatedPrice(newAvgCost * newQuantity);
                    holding.setProfit(0f);
                }
                
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
            
            // 평가금액과 손익 계산
            try {
                OrderBookResponse orderBook = orderBookService.getOrderBook(stock.getStockCode());
                float currentPrice = orderBook.getCurrentPrice();
                newHolding.setEvaluatedPrice(currentPrice * quantityChange);
                newHolding.setProfit((currentPrice - price.floatValue()) * quantityChange);
            } catch (Exception e) {
                log.warn("주가 조회 실패, 평균 매입가로 대체 - 종목: {}", stock.getStockCode());
                newHolding.setEvaluatedPrice(price.floatValue() * quantityChange);
                newHolding.setProfit(0f);
            }
            
            groupHoldingCacheRepository.save(newHolding);
        }
    }

    /**
     * 새로운 평균 매입가 계산
     */
    private float calculateNewAverageCost(float currentQuantity, float currentAvgCost, 
                                        float newQuantity, float newPrice) {
        if (currentQuantity <= 0) return newPrice;
        
        float totalCost = currentQuantity * currentAvgCost + newQuantity * newPrice;
        float totalQuantity = currentQuantity + newQuantity;
        
        return totalCost / totalQuantity;
    }

    /**
     * 그룹 거래 히스토리 저장 (주석)
     */
    private void saveGroupTradingHistory(UUID groupId, UUID stockId, float quantity, 
                                       BigDecimal price, String transactionType, 
                                       List<Order> executedOrders) {
        log.info("🔍 그룹 거래 히스토리 저장 시작 - 그룹ID: {}, 주식ID: {}, 수량: {}, 가격: {}", 
                groupId, stockId, quantity, price);
        try {
            // 주식 정보 조회
            Stock stock = stockRepository.findById(stockId)
                    .orElseThrow(() -> new BusinessException("주식을 찾을 수 없습니다.", "STOCK_NOT_FOUND"));
            
            History history = new History();
            history.setInvestmentAccount(null);
            history.setStock(stock);
            history.setTransactionType(History.TransactionType.valueOf(transactionType));
            history.setQuantity(quantity);
            history.setPrice(price);
            history.setTotalAmount(price.multiply(BigDecimal.valueOf(quantity)));
            history.setGroupId(groupId);
            history.setHistoryCategory("TRADE");
            history.setHistoryType("TRADE_EXECUTED");
            history.setTitle(String.format("%s %.2f주 %d원 %s",
                    stock.getStockName(), quantity, price.intValue(),
                    "BUY".equals(transactionType) ? "매수" : "매도"));
            // 🔥 그룹 총 예수금 잔액 조회 (거래 체결 시점의 잔액)
            int groupTotalBalance = getGroupTotalBalance(groupId);
            
            // 🔥 accountBalance를 payload에 포함
            history.setPayload(String.format("{\"groupTrading\":true,\"stockName\":\"%s\",\"quantity\":%.2f,\"price\":%d,\"accountBalance\":%d}",
                    stock.getStockName(), quantity, price.intValue(), groupTotalBalance));

            if(!executedOrders.isEmpty()) {
                history.setOrderId(executedOrders.get(0).getOrderId());
            }

            History savedHistory = historyRepository.save(history);
            
            log.info("💰 그룹 거래 체결 히스토리 저장 완료 - historyId: {}, 그룹ID: {}, 종목: {}, 수량: {}, 가격: {}, 그룹잔액: {}", 
                    savedHistory.getHistoryId(), groupId, stock.getStockName(), quantity, price, groupTotalBalance);
                    
        } catch (Exception e) {
            log.error("그룹 거래 히스토리 저장 실패 - 그룹ID: {}, 주식ID: {} - {}", 
                    groupId, stockId, e.getMessage());
            // 히스토리 저장 실패는 거래 자체를 중단시키지 않음
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
            
            // 처음 구매한 가격 대비 변동 정보 (평균 매입가 기준)
            float changeAmount = (currentPrice - holding.getAvgCost()) * holding.getTotalQuantity();
            float changeRate = holding.getAvgCost() > 0 ? 
                    ((currentPrice - holding.getAvgCost()) / holding.getAvgCost()) * 100 : 0;
            
            // 변동 방향 (평균 매입가 대비)
            String changeDirection;
            if (changeAmount > 0) {
                changeDirection = "up";
            } else if (changeAmount < 0) {
                changeDirection = "down";
            } else {
                changeDirection = "unchanged";
            }
            
            // 멤버당 평균 보유 수량 계산
            float avgQuantityPerMember = holding.getMemberCount() > 0 ? 
                    holding.getTotalQuantity() / holding.getMemberCount() : 0;
            
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
        
        // 예수금은 summary에서 처리하므로 holdings에서는 제거
        
        log.info("그룹 보유종목 조회 완료 - 그룹ID: {}, 보유종목 수: {}", groupId, responses.size());
        return responses;
    }

    /**
     * 그룹 멤버들의 총 예수금 계산
     * @param groupId 그룹 ID
     * @return 그룹 전체 예수금
     */
    private Float calculateGroupTotalCash(UUID groupId) {
        try {
            List<InvestmentAccount> groupMembers = getGroupMembers(groupId);
            float totalCash = 0.0f;
            
            for (InvestmentAccount member : groupMembers) {
                try {
                    // 각 멤버의 예수금 조회
                    BigDecimal memberBalance = portfolioCalculationService.getUserBalanceWithCache(member.getUserId());
                    totalCash += memberBalance.floatValue();
                } catch (Exception e) {
                    log.warn("멤버 예수금 조회 실패 - 사용자ID: {} - {}", member.getUserId(), e.getMessage());
                    // 개별 멤버 조회 실패해도 전체 계산은 계속 진행
                }
            }
            
            log.info("그룹 예수금 계산 완료 - 그룹ID: {}, 총 예수금: {}", groupId, totalCash);
            return totalCash;
            
        } catch (Exception e) {
            log.error("그룹 예수금 계산 실패 - 그룹ID: {} - {}", groupId, e.getMessage());
            return 0.0f;
        }
    }

    /**
     * 그룹 포트폴리오 요약 정보 계산
     * @param groupId 그룹 ID
     * @return 그룹 포트폴리오 요약 정보
     */
    @Transactional(readOnly = true)
    public PortfolioSummaryResponse calculateGroupPortfolioSummary(UUID groupId) {
        log.info("그룹 포트폴리오 요약 계산 - 그룹ID: {}", groupId);
        
        // 그룹의 보유 수량이 0보다 큰 종목들만 조회
        List<GroupHoldingCache> groupHoldings = groupHoldingCacheRepository
                .findByGroupIdAndTotalQuantityGreaterThan(groupId, 0);
        
        BigDecimal totalInvestedBD = BigDecimal.ZERO;
        BigDecimal totalValueBD = BigDecimal.ZERO;
        
        for (GroupHoldingCache holding : groupHoldings) {
            BigDecimal avgCost = BigDecimal.valueOf(holding.getAvgCost());
            BigDecimal quantity = BigDecimal.valueOf(holding.getTotalQuantity());
            
            totalInvestedBD = totalInvestedBD.add(avgCost.multiply(quantity));
            
            // evaluatedPrice가 null이거나 0이면 실시간으로 계산
            Float evaluatedPrice = holding.getEvaluatedPrice();
            if (evaluatedPrice == null || evaluatedPrice == 0) {
                try {
                    OrderBookResponse orderBook = orderBookService.getOrderBook(holding.getStock().getStockCode());
                    evaluatedPrice = orderBook.getCurrentPrice() * holding.getTotalQuantity();
                    log.info("💰 실시간 evaluatedPrice 계산: {}원 ({} × {})", 
                            evaluatedPrice, orderBook.getCurrentPrice(), holding.getTotalQuantity());
                } catch (Exception e) {
                    log.warn("⚠️ 실시간 가격 조회 실패, 평균 매입가로 대체: {}", e.getMessage());
                    evaluatedPrice = holding.getAvgCost() * holding.getTotalQuantity();
                }
            }
            totalValueBD = totalValueBD.add(BigDecimal.valueOf(evaluatedPrice != null ? evaluatedPrice : 0));
        }
        
        BigDecimal totalProfitBD = totalValueBD.subtract(totalInvestedBD);
        BigDecimal totalProfitRateBD = totalInvestedBD.compareTo(BigDecimal.ZERO) > 0 ? 
            totalProfitBD.divide(totalInvestedBD, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : 
            BigDecimal.ZERO;
        
        // 금액은 정수로 반올림, 수익률은 소수점 2자리로 반올림
        float totalInvested = totalInvestedBD.setScale(0, RoundingMode.HALF_UP).floatValue();
        float totalValue = totalValueBD.setScale(0, RoundingMode.HALF_UP).floatValue();
        float totalProfit = totalProfitBD.setScale(0, RoundingMode.HALF_UP).floatValue();
        float totalProfitRate = totalProfitRateBD.setScale(2, RoundingMode.HALF_UP).floatValue();
        
        // 모든 보유 종목 (평가금액 기준 내림차순 정렬)
        List<HoldingResponse> topHoldings = groupHoldings.stream()
                .sorted((h1, h2) -> Float.compare(
                    (h2.getEvaluatedPrice() != null ? h2.getEvaluatedPrice() : 0),
                    (h1.getEvaluatedPrice() != null ? h1.getEvaluatedPrice() : 0)
                ))
                .map(holding -> convertToHoldingResponse(holding))
                .collect(Collectors.toList());
        
        // 그룹 멤버들의 총 예수금 계산
        float totalCashBalance = calculateGroupTotalCash(groupId);
        
        return new PortfolioSummaryResponse(
                totalInvested,
                totalValue,
                totalProfit,
                totalProfitRate,
                groupHoldings.size(),
                topHoldings,
                totalCashBalance
        );
    }

    /**
     * GroupHoldingCache를 HoldingResponse로 변환 (PortfolioSummaryResponse용)
     */
    private HoldingResponse convertToHoldingResponse(GroupHoldingCache holding) {
        Stock stock = holding.getStock();
        
        // 현재가 조회 (OrderBookService 사용)
        OrderBookResponse orderBook = orderBookService.getOrderBook(stock.getStockCode());
        Float currentPrice = orderBook.getCurrentPrice();
        
        // 평가금액 및 수익률 계산 (BigDecimal 사용으로 정밀도 개선)
        BigDecimal currentPriceBD = BigDecimal.valueOf(currentPrice);
        BigDecimal quantityBD = BigDecimal.valueOf(holding.getTotalQuantity());
        BigDecimal avgCostBD = BigDecimal.valueOf(holding.getAvgCost());
        
        BigDecimal evaluatedPriceBD = currentPriceBD.multiply(quantityBD);
        BigDecimal totalCostBD = avgCostBD.multiply(quantityBD);
        BigDecimal profitBD = evaluatedPriceBD.subtract(totalCostBD);
        BigDecimal profitRateBD = totalCostBD.compareTo(BigDecimal.ZERO) > 0 ? 
            profitBD.divide(totalCostBD, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : 
            BigDecimal.ZERO;
        
        // 처음 구매한 가격 대비 변동 정보 (평균 매입가 기준)
        BigDecimal changeAmountBD = currentPriceBD.subtract(avgCostBD).multiply(quantityBD);
        BigDecimal changeRateBD = avgCostBD.compareTo(BigDecimal.ZERO) > 0 ? 
            currentPriceBD.subtract(avgCostBD).divide(avgCostBD, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : 
            BigDecimal.ZERO;
        
        // 금액은 정수로 반올림, 수익률은 소수점 2자리로 반올림
        float evaluatedPrice = evaluatedPriceBD.setScale(0, RoundingMode.HALF_UP).floatValue();
        float totalCost = totalCostBD.setScale(0, RoundingMode.HALF_UP).floatValue();
        float profit = profitBD.setScale(0, RoundingMode.HALF_UP).floatValue();
        float profitRate = profitRateBD.setScale(2, RoundingMode.HALF_UP).floatValue();
        float changeAmount = changeAmountBD.setScale(0, RoundingMode.HALF_UP).floatValue();
        float changeRate = changeRateBD.setScale(2, RoundingMode.HALF_UP).floatValue();
        
        // 변동 방향 (평균 매입가 대비)
        String changeDirection;
        if (changeAmount > 0) {
            changeDirection = "up";
        } else if (changeAmount < 0) {
            changeDirection = "down";
        } else {
            changeDirection = "unchanged";
        }
        
        return new HoldingResponse(
                holding.getGroupHoldingId(), // GroupHoldingCache의 ID를 holdingId로 사용
                stock.getId(),
                stock.getStockCode(),
                stock.getStockName(),
                stock.getStockImage(),
                holding.getTotalQuantity(),
                avgCostBD.setScale(0, RoundingMode.HALF_UP).floatValue(), // 매입금액도 정수로 반올림
                currentPrice,
                changeAmount,
                changeRate,
                profit,
                evaluatedPrice,
                profitRate,
                changeDirection
        );
    }

    /**
     * GroupHoldingCache를 GroupHoldingResponse로 변환
     */
    private GroupHoldingResponse convertToGroupHoldingResponse(GroupHoldingCache holding) {
        Stock stock = holding.getStock();
        
        // 현재가 조회 (OrderBookService 사용)
        OrderBookResponse orderBook = orderBookService.getOrderBook(stock.getStockCode());
        Float currentPrice = orderBook.getCurrentPrice();
        
        // 평가금액 및 수익률 계산 (BigDecimal 사용으로 정밀도 개선)
        BigDecimal currentPriceBD = BigDecimal.valueOf(currentPrice);
        BigDecimal quantityBD = BigDecimal.valueOf(holding.getTotalQuantity());
        BigDecimal avgCostBD = BigDecimal.valueOf(holding.getAvgCost());
        
        BigDecimal evaluatedPriceBD = currentPriceBD.multiply(quantityBD);
        BigDecimal totalCostBD = avgCostBD.multiply(quantityBD);
        BigDecimal profitBD = evaluatedPriceBD.subtract(totalCostBD);
        BigDecimal profitRateBD = totalCostBD.compareTo(BigDecimal.ZERO) > 0 ? 
            profitBD.divide(totalCostBD, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : 
            BigDecimal.ZERO;
        
        // 처음 구매한 가격 대비 변동 정보 (평균 매입가 기준)
        BigDecimal changeAmountBD = currentPriceBD.subtract(avgCostBD).multiply(quantityBD);
        BigDecimal changeRateBD = avgCostBD.compareTo(BigDecimal.ZERO) > 0 ? 
            currentPriceBD.subtract(avgCostBD).divide(avgCostBD, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : 
            BigDecimal.ZERO;
        
        // 금액은 정수로 반올림, 수익률은 소수점 2자리로 반올림
        float evaluatedPrice = evaluatedPriceBD.setScale(0, RoundingMode.HALF_UP).floatValue();
        float totalCost = totalCostBD.setScale(0, RoundingMode.HALF_UP).floatValue();
        float profit = profitBD.setScale(0, RoundingMode.HALF_UP).floatValue();
        float profitRate = profitRateBD.setScale(2, RoundingMode.HALF_UP).floatValue();
        float changeAmount = changeAmountBD.setScale(0, RoundingMode.HALF_UP).floatValue();
        float changeRate = changeRateBD.setScale(2, RoundingMode.HALF_UP).floatValue();
        
        // 변동 방향 (평균 매입가 대비)
        String changeDirection;
        if (changeAmount > 0) {
            changeDirection = "up";
        } else if (changeAmount < 0) {
            changeDirection = "down";
        } else {
            changeDirection = "unchanged";
        }
        
        // 멤버당 평균 보유 수량 계산
        float avgQuantityPerMember = holding.getMemberCount() > 0 ? 
                holding.getTotalQuantity() / holding.getMemberCount() : 0;
        
        return new GroupHoldingResponse(
                holding.getGroupHoldingId(),
                holding.getGroupId(),
                stock.getId(),
                stock.getStockCode(),
                stock.getStockName(),
                stock.getStockImage(),
                holding.getTotalQuantity(),
                avgCostBD.setScale(0, RoundingMode.HALF_UP).floatValue(), // 매입금액도 정수로 반올림
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
    }

    /**
     * 그룹 총 예수금 잔액 조회
     * - 투표를 통해 체결된 거래의 경우 그룹 전체 잔액을 히스토리에 포함
     */
    private int getGroupTotalBalance(UUID groupId) {
        try {
            // 1. 그룹 멤버들의 투자 계좌 정보 조회
            List<InvestmentAccountDto> accounts = userServiceClient.getGroupMemberAccounts(groupId);
            
            if (accounts.isEmpty()) {
                log.warn("그룹 멤버 계좌가 없음 - groupId: {}", groupId);
                return 0;
            }
            
            // 2. 각 계좌의 예수금 잔액 조회 및 합산
            int totalBalance = 0;
            for (InvestmentAccountDto account : accounts) {
                try {
                    Optional<BalanceCache> balanceOpt = balanceCacheRepository
                            .findByAccountId(account.getInvestmentAccountId());
                    if (balanceOpt.isPresent()) {
                        totalBalance += balanceOpt.get().getBalance();
                    }
                } catch (Exception e) {
                    log.warn("계좌 잔액 조회 실패 - accountId: {} - {}", 
                            account.getInvestmentAccountId(), e.getMessage());
                }
            }
            
            log.info("💰 그룹 총 예수금 잔액 조회 완료 - groupId: {}, 멤버수: {}, 총잔액: {}", 
                    groupId, accounts.size(), totalBalance);
            return totalBalance;
            
        } catch (Exception e) {
            log.error("그룹 총 잔액 조회 실패 - groupId: {} - {}", groupId, e.getMessage());
            return 0;
        }
    }
}
