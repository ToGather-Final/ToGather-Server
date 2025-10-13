package com.example.trading_service.service;

import com.example.trading_service.domain.*;
import com.example.trading_service.dto.*;
import com.example.trading_service.exception.*;
import com.example.trading_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TradingService {

    private final InvestmentAccountRepository investmentAccountRepository;
    private final BalanceCacheRepository balanceCacheRepository;
    private final HoldingCacheRepository holdingCacheRepository;
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final StockRepository stockRepository;

    // 투자 계좌 개설
    public UUID createInvestmentAccount(UUID userId) {
        // 이미 계좌가 있는지 확인
        if (investmentAccountRepository.existsByUserId(userId.toString())) {
            throw new IllegalArgumentException("이미 투자 계좌가 존재합니다.");
        }

        // 계좌 생성
        InvestmentAccount account = new InvestmentAccount();
        account.setUserId(userId.toString());
        account.setAccountNo(generateAccountNumber());
        
        InvestmentAccount savedAccount = investmentAccountRepository.save(account);
        
        // 초기 잔고 생성
        BalanceCache balance = new BalanceCache();
        balance.setInvestmentAccountId(savedAccount.getInvestmentAccountId());
        balance.setBalance(0);
        balanceCacheRepository.save(balance);
        
        log.info("투자 계좌가 생성되었습니다. 사용자: {}, 계좌번호: {}", userId, savedAccount.getAccountNo());
        return savedAccount.getInvestmentAccountId();
    }

    // 주식 매수
    public void buyStock(UUID userId, BuyRequest request) {
        // 1️⃣ 투자 계좌 조회
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        // 2️⃣ 주식 정보 조회 및 검증
        Stock stock = stockRepository.findById(request.getStockId())
                .orElseThrow(() -> new InvalidOrderException("존재하지 않는 주식입니다."));
        
        if (!stock.isEnabled()) {
            throw new InvalidOrderException("거래가 정지된 주식입니다: " + stock.getStockName() + "(" + stock.getStockCode() + ")");
        }

        // 3️⃣ 주문 수량 및 가격 검증
        validateBuyOrder(request);

        // 4️⃣ 잔고 확인
        BalanceCache balance = balanceCacheRepository.findByInvestmentAccountId(account.getInvestmentAccountId())
                .orElseThrow(() -> new InvalidOrderException("잔고 정보를 찾을 수 없습니다."));

        long totalAmount = (long) (request.getPrice() * request.getQuantity());
        
        if (balance.getBalance() < totalAmount) {
            throw new InsufficientBalanceException(totalAmount, balance.getBalance());
        }

        // 주문 생성
        Order order = new Order();
        order.setInvestmentAccountId(account.getInvestmentAccountId());
        order.setStockId(request.getStockId());
        order.setOrderType(Order.OrderType.BUY);
        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());
        order.setStatus(Order.Status.PENDING);
        
        Order savedOrder = orderRepository.save(order);

        // 시장가 주문인 경우 즉시 체결 처리
        if (request.getIsMarketOrder()) {
            processMarketOrder(savedOrder);
        }

        log.info("매수 주문이 생성되었습니다. 사용자: {}, 종목: {}, 수량: {}, 가격: {}", 
                userId, stock.getStockName(), request.getQuantity(), request.getPrice());
    }

    // 주식 매도
    public void sellStock(UUID userId, SellRequest request) {
        // 1️⃣ 투자 계좌 조회
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        // 2️⃣ 주식 정보 조회 및 검증
        Stock stock = stockRepository.findById(request.getStockId())
                .orElseThrow(() -> new InvalidOrderException("존재하지 않는 주식입니다."));
        
        if (!stock.isEnabled()) {
            throw new InvalidOrderException("거래가 정지된 주식입니다: " + stock.getStockName() + "(" + stock.getStockCode() + ")");
        }

        // 3️⃣ 주문 수량 및 가격 검증
        validateSellOrder(request);

        // 4️⃣ 보유 종목 확인
        HoldingCache holding = holdingCacheRepository
                .findByInvestmentAccountIdAndStockId(account.getInvestmentAccountId(), request.getStockId())
                .orElseThrow(() -> new InsufficientStockException(stock.getStockName(), request.getQuantity(), 0));

        if (holding.getQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(stock.getStockName(), request.getQuantity(), holding.getQuantity());
        }

        // 주문 생성
        Order order = new Order();
        order.setInvestmentAccountId(account.getInvestmentAccountId());
        order.setStockId(request.getStockId());
        order.setOrderType(Order.OrderType.SELL);
        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());
        order.setStatus(Order.Status.PENDING);
        
        Order savedOrder = orderRepository.save(order);

        // 시장가 주문인 경우 즉시 체결 처리
        if (request.getIsMarketOrder()) {
            processMarketOrder(savedOrder);
        }

        log.info("매도 주문이 생성되었습니다. 사용자: {}, 종목: {}, 수량: {}, 가격: {}", 
                userId, request.getStockId(), request.getQuantity(), request.getPrice());
    }

    // 예수금 충전
    public void depositFunds(UUID userId, DepositRequest request) {
        // 투자 계좌 조회
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        // 잔고 업데이트
        BalanceCache balance = balanceCacheRepository.findByInvestmentAccountId(account.getInvestmentAccountId())
                .orElseThrow(() -> new IllegalArgumentException("잔고 정보를 찾을 수 없습니다."));
        
        balance.setBalance(balance.getBalance() + request.getAmount().intValue());
        balanceCacheRepository.save(balance);
        
        log.info("예수금이 충전되었습니다. 사용자: {}, 충전 금액: {}", userId, request.getAmount());
    }

    // 보유 종목 조회
    @Transactional(readOnly = true)
    public List<HoldingResponse> getUserHoldings(UUID userId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        List<HoldingCache> holdings = holdingCacheRepository
                .findByInvestmentAccountIdAndQuantityGreaterThan(account.getInvestmentAccountId(), 0);
        
        return holdings.stream()
                .map(this::convertToHoldingResponse)
                .collect(Collectors.toList());
    }

    // 계좌 잔고 조회
    @Transactional(readOnly = true)
    public BalanceResponse getAccountBalance(UUID userId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        BalanceCache balance = balanceCacheRepository.findByInvestmentAccountId(account.getInvestmentAccountId())
                .orElseThrow(() -> new IllegalArgumentException("잔고 정보를 찾을 수 없습니다."));

        // 보유 종목들의 총 평가금액 계산
        List<HoldingCache> holdings = holdingCacheRepository.findByInvestmentAccountId(account.getInvestmentAccountId());
        
        float totalInvested = 0;
        float totalValue = 0;
        
        for (HoldingCache holding : holdings) {
            totalInvested += holding.getAvgCost() * holding.getQuantity();
            totalValue += holding.getEvaluatedPrice() != null ? holding.getEvaluatedPrice() : 0;
        }
        
        float totalProfit = totalValue - totalInvested;
        float totalProfitRate = totalInvested > 0 ? (totalProfit / totalInvested) * 100 : 0;
        
        return new BalanceResponse(
                balance.getBalanceId(),
                balance.getInvestmentAccountId(),
                (long) balance.getBalance(),
                totalInvested,
                totalValue,
                totalProfit,
                totalProfitRate
        );
    }

    // 거래 내역 조회
    @Transactional(readOnly = true)
    public List<TradeHistoryResponse> getTradeHistory(UUID userId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        List<Trade> trades = tradeRepository.findByInvestmentAccountId(account.getInvestmentAccountId());
        
        return trades.stream()
                .map(this::convertToTradeHistoryResponse)
                .collect(Collectors.toList());
    }

    // 주식 조회
    @Transactional(readOnly = true)
    public List<StockResponse> getStocks(String search) {
        List<Stock> stocks;
        
        if (search != null && !search.trim().isEmpty()) {
            stocks = stockRepository.searchStocks(search.trim());
        } else {
            stocks = stockRepository.findByEnabledTrue();
        }
        
        return stocks.stream()
                .map(this::convertToStockResponse)
                .collect(Collectors.toList());
    }

    // 특정 주식 상세 조회
    @Transactional(readOnly = true)
    public StockResponse getStockDetail(UUID stockId) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주식입니다."));
        
        return convertToStockResponse(stock);
    }

    // 시장가 주문 처리
    private void processMarketOrder(Order order) {
        // 실제 거래소에서는 실시간 가격을 가져와야 하지만, 
        // 여기서는 주문 가격으로 즉시 체결 처리
        executeTrade(order, order.getPrice());
    }

    // 체결 처리
    private void executeTrade(Order order, float executionPrice) {
        // 체결 기록 생성
        Trade trade = Trade.builder()
                .orderId(order.getOrderId())
                .stockCode(order.getStockCode())
                .stockName(order.getStockName())
                .quantity(order.getQuantity())
                .price(executionPrice)
                .tradeType(order.getOrderType())
                .build();
        tradeRepository.save(trade);

        // 주문 상태 업데이트
        order.setStatus(Order.Status.FILLED);
        orderRepository.save(order);

        // 잔고 및 보유 종목 업데이트
        updateAccountAfterTrade(order, executionPrice);
    }

    // 거래 후 계좌 업데이트
    private void updateAccountAfterTrade(Order order, float executionPrice) {
        float totalAmount = executionPrice * order.getQuantity();
        
        if (order.getOrderType() == Order.OrderType.BUY) {
            // 매수: 잔고 차감, 보유 종목 추가/업데이트
            updateBalance(order.getInvestmentAccountId(), -totalAmount);
            updateHolding(order.getInvestmentAccountId(), order.getStockId(), (int) order.getQuantity(), executionPrice, true);
        } else {
            // 매도: 잔고 증가, 보유 종목 차감
            updateBalance(order.getInvestmentAccountId(), totalAmount);
            updateHolding(order.getInvestmentAccountId(), order.getStockId(), (int) order.getQuantity(), executionPrice, false);
        }
    }

    // 잔고 업데이트
    private void updateBalance(UUID accountId, float amount) {
        BalanceCache balance = balanceCacheRepository.findByInvestmentAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("잔고 정보를 찾을 수 없습니다."));
        
        balance.setBalance(balance.getBalance() + (int) amount);
        balanceCacheRepository.save(balance);
    }

    // 보유 종목 업데이트
    private void updateHolding(UUID accountId, UUID stockId, int quantity, float price, boolean isBuy) {
        Optional<HoldingCache> existingHolding = holdingCacheRepository
                .findByInvestmentAccountIdAndStockId(accountId, stockId);
        
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
                newHolding.setInvestmentAccountId(accountId);
                newHolding.setStockId(stockId);
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

    // 헬퍼 메서드들
    private InvestmentAccount getInvestmentAccountByUserId(UUID userId) {
        return investmentAccountRepository.findByUserId(userId.toString())
                .orElseThrow(() -> new IllegalArgumentException("투자 계좌를 찾을 수 없습니다."));
    }

    private String generateAccountNumber() {
        return "INV" + System.currentTimeMillis();
    }

    private HoldingResponse convertToHoldingResponse(HoldingCache holding) {
        // 실제로는 현재가를 가져와야 하지만, 여기서는 평균 단가로 대체
        float currentPrice = holding.getAvgCost();
        float evaluatedPrice = currentPrice * holding.getQuantity();
        float profit = evaluatedPrice - (holding.getAvgCost() * holding.getQuantity());
        float profitRate = (holding.getAvgCost() * holding.getQuantity()) > 0 ? 
                (profit / (holding.getAvgCost() * holding.getQuantity())) * 100 : 0;

        return new HoldingResponse(
                holding.getHoldingId(),
                holding.getStockId(),
                "", // stockCode - 실제로는 Stock 테이블에서 조회 필요
                "", // stockName - 실제로는 Stock 테이블에서 조회 필요
                holding.getQuantity(),
                holding.getAvgCost(),
                currentPrice,
                profit,
                evaluatedPrice,
                profitRate
        );
    }

    private TradeHistoryResponse convertToTradeHistoryResponse(Trade trade) {
        // Order 정보를 가져와야 하지만, 간단히 처리
        return new TradeHistoryResponse(
                trade.getTradeId(),
                null, // stockId - Order에서 조회 필요
                "", // stockCode
                "", // stockName
                "BUY", // orderType - Order에서 조회 필요
                (int) trade.getQuantity(),
                trade.getPrice(),
                trade.getCreatedAt(),
                "FILLED"
        );
    }

    private StockResponse convertToStockResponse(Stock stock) {
        // 실제로는 현재가 정보를 가져와야 하지만, 여기서는 기본값 사용
        return new StockResponse(
                stock.getId(),
                stock.getStockCode(),
                stock.getStockName(),
                stock.getStockImage(),
                stock.getCountry().toString(),
                0.0f, // currentPrice
                0.0f, // changeAmount
                0.0f, // changeRate
                stock.isEnabled()
        );
    }

    // 대기 중인 주문 조회
    @Transactional(readOnly = true)
    public List<OrderResponse> getPendingOrders(UUID userId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        List<Order> orders = orderRepository.findPendingOrdersByAccountId(account.getInvestmentAccountId());
        
        return orders.stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
    }

    // 주문 취소
    public void cancelOrder(UUID userId, UUID orderId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        
        if (!order.getInvestmentAccountId().equals(account.getInvestmentAccountId())) {
            throw new IllegalArgumentException("권한이 없는 주문입니다.");
        }
        
        if (order.getStatus() != Order.Status.PENDING) {
            throw new IllegalArgumentException("취소할 수 없는 주문입니다.");
        }
        
        order.setStatus(Order.Status.CANCELLED);
        orderRepository.save(order);
        
        log.info("주문이 취소되었습니다. 사용자: {}, 주문ID: {}", userId, orderId);
    }

    // 계좌 정보 조회
    @Transactional(readOnly = true)
    public AccountInfoResponse getAccountInfo(UUID userId) {
        Optional<InvestmentAccount> accountOpt = investmentAccountRepository.findByUserId(userId.toString());
        
        if (accountOpt.isPresent()) {
            InvestmentAccount account = accountOpt.get();
            return new AccountInfoResponse(
                    account.getInvestmentAccountId(),
                    account.getAccountNo(),
                    account.getUserId(),
                    account.getCreatedAt(),
                    true
            );
        } else {
            return new AccountInfoResponse(null, null, userId.toString(), null, false);
        }
    }

    // 특정 종목 거래 내역 조회
    @Transactional(readOnly = true)
    public List<TradeHistoryResponse> getStockTradeHistory(UUID userId, UUID stockId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        List<Trade> trades = tradeRepository.findByInvestmentAccountIdAndStockId(account.getInvestmentAccountId(), stockId);
        
        return trades.stream()
                .map(this::convertToTradeHistoryResponse)
                .collect(Collectors.toList());
    }

    // 포트폴리오 요약 정보 조회
    @Transactional(readOnly = true)
    public PortfolioSummaryResponse getPortfolioSummary(UUID userId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        List<HoldingCache> holdings = holdingCacheRepository
                .findByInvestmentAccountIdAndQuantityGreaterThan(account.getInvestmentAccountId(), 0);
        
        float totalInvested = 0;
        float totalValue = 0;
        
        for (HoldingCache holding : holdings) {
            totalInvested += holding.getAvgCost() * holding.getQuantity();
            totalValue += holding.getEvaluatedPrice() != null ? holding.getEvaluatedPrice() : 0;
        }
        
        float totalProfit = totalValue - totalInvested;
        float totalProfitRate = totalInvested > 0 ? (totalProfit / totalInvested) * 100 : 0;
        
        // 상위 5개 보유 종목
        List<HoldingResponse> topHoldings = holdings.stream()
                .sorted((h1, h2) -> Float.compare(
                    (h2.getEvaluatedPrice() != null ? h2.getEvaluatedPrice() : 0),
                    (h1.getEvaluatedPrice() != null ? h1.getEvaluatedPrice() : 0)
                ))
                .limit(5)
                .map(this::convertToHoldingResponse)
                .collect(Collectors.toList());
        
        return new PortfolioSummaryResponse(
                totalInvested,
                totalValue,
                totalProfit,
                totalProfitRate,
                holdings.size(),
                topHoldings
        );
    }

    // OrderResponse 변환
    private OrderResponse convertToOrderResponse(Order order) {
        // 실제로는 Stock 정보를 가져와야 하지만, 간단히 처리
        return new OrderResponse(
                order.getOrderId(),
                order.getStockId(),
                "", // stockCode - Stock에서 조회 필요
                "", // stockName - Stock에서 조회 필요
                order.getOrderType().toString(),
                (int) order.getQuantity(),
                order.getPrice(),
                order.getStatus().toString(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    /**
     * 주문을 체결 처리하는 메서드
     * @param order 체결할 주문
     * @param executedPrice 체결가
     */
    @Transactional
    public void executeOrder(Order order, long executedPrice) {
        // 1️⃣ Trade 생성
        Trade trade = Trade.builder()
                .orderId(order.getOrderId())
                .stockCode(order.getStockCode())
                .stockName(order.getStockName())
                .quantity(order.getQuantity())
                .price(executedPrice)
                .tradeType(order.getOrderType())
                .build();
        tradeRepository.save(trade);

        // 2️⃣ BalanceCache 업데이트
        updateBalanceCache(order, executedPrice);

        // 3️⃣ HoldingCache 업데이트
        updateHoldingCache(order, executedPrice);

        System.out.printf("✅ 체결 완료 → %s (%s) %d주 @ %d원\n",
                order.getStockName(), order.getStockCode(),
                (int) order.getQuantity(), executedPrice);
    }

    /**
     * BalanceCache 업데이트 로직
     */
    private void updateBalanceCache(Order order, long executedPrice) {
        BalanceCache balance = balanceCacheRepository
                .findByInvestmentAccountId(order.getInvestmentAccountId())
                .orElseThrow(() -> new InvalidOrderException("잔고 정보를 찾을 수 없습니다."));

        long totalAmount = (long) (executedPrice * order.getQuantity());

        if (order.isBuy()) {
            // 매수 시 잔고 부족 체크
            if (balance.getBalance() < totalAmount) {
                throw new InsufficientBalanceException(totalAmount, balance.getBalance());
            }
            balance.setBalance(balance.getBalance() - (int) totalAmount);
            System.out.printf("📉 잔액 차감: -%d원 (잔액: %d원)\n", totalAmount, balance.getBalance());
        } else {
            balance.setBalance(balance.getBalance() + (int) totalAmount);
            System.out.printf("📈 잔액 증가: +%d원 (잔액: %d원)\n", totalAmount, balance.getBalance());
        }
        balanceCacheRepository.save(balance);
    }

    /**
     * HoldingCache 업데이트 로직
     */
    private void updateHoldingCache(Order order, long executedPrice) {
        // Stock 정보 조회
        Stock stock = stockRepository.findById(order.getStockId())
                .orElseThrow(() -> new RuntimeException("Stock not found"));

        HoldingCache holding = holdingCacheRepository
                .findByInvestmentAccountIdAndStockId(order.getInvestmentAccountId(), order.getStockId())
                .orElse(null);

        if (order.isBuy()) {
            // 매수: 보유 수량 증가
            if (holding == null) {
                // 새로운 보유 종목 생성
                holding = new HoldingCache();
                holding.setInvestmentAccountId(order.getInvestmentAccountId());
                holding.setStockId(order.getStockId());
                holding.setQuantity((int) order.getQuantity());
                holding.setAvgCost(executedPrice);
                System.out.printf("📈 신규 보유: %s %d주 (평균단가: %d원)\n", 
                    stock.getStockName(), (int) order.getQuantity(), executedPrice);
            } else {
                // 기존 보유 종목 수량 증가 및 평균단가 재계산
                int newQuantity = holding.getQuantity() + (int) order.getQuantity();
                float newAvgCost = ((holding.getAvgCost() * holding.getQuantity()) + 
                                   (executedPrice * order.getQuantity())) / newQuantity;
                
                holding.setQuantity(newQuantity);
                holding.setAvgCost(newAvgCost);
                System.out.printf("📈 보유 수량 증가: %s +%d주 (총 %d주, 평균단가: %.0f원)\n", 
                    stock.getStockName(), (int) order.getQuantity(), newQuantity, newAvgCost);
            }
        } else {
            // 매도: 보유 수량 감소
            if (holding == null || holding.getQuantity() < order.getQuantity()) {
                throw new InsufficientStockException(stock.getStockName(), (int) order.getQuantity(), 
                    holding != null ? holding.getQuantity() : 0);
            }
            
            int newQuantity = holding.getQuantity() - (int) order.getQuantity();
            holding.setQuantity(newQuantity);
            
            if (newQuantity == 0) {
                holdingCacheRepository.delete(holding);
                System.out.printf("📉 보유 종목 완전 매도: %s %d주\n", 
                    stock.getStockName(), (int) order.getQuantity());
                return;
            } else {
                System.out.printf("📉 보유 수량 감소: %s -%d주 (잔여 %d주)\n", 
                    stock.getStockName(), (int) order.getQuantity(), newQuantity);
            }
        }

        holdingCacheRepository.save(holding);
    }

    /**
     * 매수 주문 검증
     */
    private void validateBuyOrder(BuyRequest request) {
        if (request.getQuantity() <= 0) {
            throw new InvalidOrderException("주문 수량은 0보다 커야 합니다.");
        }

        if (request.getPrice() <= 0) {
            throw new InvalidOrderException("주문 가격은 0보다 커야 합니다.");
        }

        // 수량이 너무 큰 경우 (예: 1억주 이상)
        if (request.getQuantity() > 100_000_000) {
            throw new InvalidOrderException("주문 수량이 너무 큽니다. (최대 1억주)");
        }

        // 가격이 너무 높은 경우 (예: 1억원 이상)
        if (request.getPrice() > 100_000_000) {
            throw new InvalidOrderException("주문 가격이 너무 높습니다. (최대 1억원)");
        }
    }

    /**
     * 매도 주문 검증
     */
    private void validateSellOrder(SellRequest request) {
        if (request.getQuantity() <= 0) {
            throw new InvalidOrderException("주문 수량은 0보다 커야 합니다.");
        }

        if (request.getPrice() <= 0) {
            throw new InvalidOrderException("주문 가격은 0보다 커야 합니다.");
        }

        // 수량이 너무 큰 경우
        if (request.getQuantity() > 100_000_000) {
            throw new InvalidOrderException("주문 수량이 너무 큽니다. (최대 1억주)");
        }

        // 가격이 너무 높은 경우
        if (request.getPrice() > 100_000_000) {
            throw new InvalidOrderException("주문 가격이 너무 높습니다. (최대 1억원)");
        }
    }
}
