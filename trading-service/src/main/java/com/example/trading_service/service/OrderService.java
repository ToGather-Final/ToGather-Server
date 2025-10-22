package com.example.trading_service.service;

import com.example.trading_service.client.VoteServiceClient;
import com.example.trading_service.domain.*;
import com.example.trading_service.dto.*;
import com.example.trading_service.exception.*;
import com.example.trading_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final InvestmentAccountRepository investmentAccountRepository;
    private final BalanceCacheRepository balanceCacheRepository;
    private final HoldingCacheRepository holdingCacheRepository;
    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;
    private final TradeExecutionService tradeExecutionService;
    private final VoteServiceClient voteServiceClient;
    // private final HistoryRepository historyRepository; // 히스토리 기능 주석

    // 주식 매수 주문 (개인 거래)
    public Order buyStock(UUID userId, BuyRequest request) {
        return buyStock(userId, request, null);
    }

    // 주식 매수 주문 (그룹 거래 포함)
    public Order buyStock(UUID userId, BuyRequest request, UUID groupId) {
        // 투자 계좌 조회
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        // 주식 정보 조회 (stockId 또는 stockCode로)
        Stock stock = getStockFromRequest(request);
        
        if (!stock.isEnabled()) {
            throw new BusinessException("거래가 중단된 종목입니다", "STOCK_DISABLED");
        }

        // 잔고 확인
        BalanceCache balance = balanceCacheRepository.findByAccountId(account.getInvestmentAccountId())
                .orElseThrow(() -> new BusinessException("잔고 정보를 찾을 수 없습니다", "BALANCE_NOT_FOUND"));

        BigDecimal totalAmount = request.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        
        if (balance.getBalance() < totalAmount.floatValue()) {
            // 그룹 거래가 아닌 경우에만 개별 히스토리 저장
            if (groupId == null) {
                saveTradeFailedHistory(account.getUserId(), stock, request, "BUY", "잔고가 부족합니다.", groupId);
            }
            
            throw new InsufficientBalanceException(totalAmount.floatValue(), balance.getBalance());
        }

        // 주문 생성
        Order order = new Order();
        order.setInvestmentAccount(account);
        order.setStock(stock);
        order.setOrderType(Order.OrderType.BUY);
        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice().floatValue());
        order.setStatus(Order.Status.PENDING);
        order.setGroupId(groupId); // 그룹 ID 설정
        
        Order savedOrder = orderRepository.save(order);

        // 시장가 주문인 경우 즉시 체결 처리
        if (request.getIsMarketOrder()) {
            tradeExecutionService.processMarketOrder(savedOrder);
        }

        log.info("매수 주문이 생성되었습니다. 사용자: {}, 종목: {}, 수량: {}, 가격: {}", 
                userId, stock.getStockName(), request.getQuantity(), request.getPrice());

        return savedOrder;
    }

    // 주식 매도 주문 (개인 거래)
    public Order sellStock(UUID userId, SellRequest request) {
        return sellStock(userId, request, null);
    }

    // 주식 매도 주문 (그룹 거래 포함)
    public Order sellStock(UUID userId, SellRequest request, UUID groupId) {
        // 투자 계좌 조회
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        // 주식 정보 조회 (stockId 또는 stockCode로)
        Stock stock = getStockFromRequest(request);
        
        // 보유 종목 확인
        HoldingCache holding = holdingCacheRepository
                .findByAccountIdAndStockId(account.getInvestmentAccountId(), stock.getId())
                .orElseThrow(() -> new BusinessException("보유하지 않은 종목입니다", "HOLDING_NOT_FOUND"));

        if (holding.getQuantity() < request.getQuantity()) {
            // 그룹 거래가 아닌 경우에만 개별 히스토리 저장
            if (groupId == null) {
                saveTradeFailedHistory(account.getUserId(), stock, request, "SELL", "보유수량이 부족합니다.", groupId);
            }
            
            throw new InsufficientHoldingException(request.getQuantity(), holding.getQuantity());
        }

        // 주문 생성
        Order order = new Order();
        order.setInvestmentAccount(account);
        order.setStock(stock);
        order.setOrderType(Order.OrderType.SELL);
        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice().floatValue());
        order.setStatus(Order.Status.PENDING);
        order.setGroupId(groupId); // 그룹 ID 설정
        
        Order savedOrder = orderRepository.save(order);

        // 시장가 주문인 경우 즉시 체결 처리
        if (request.getIsMarketOrder()) {
            tradeExecutionService.processMarketOrder(savedOrder);
        }

        log.info("매도 주문이 생성되었습니다. 사용자: {}, 종목: {}, 수량: {}, 가격: {}", 
                userId, request.getStockId(), request.getQuantity(), request.getPrice());

        return savedOrder;
    }

    // 대기 중인 주문 조회
    @Transactional(readOnly = true)
    // 전체 주문 조회 (모든 상태)
    public List<OrderResponse> getAllOrders(UUID userId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        List<Order> orders = orderRepository.findByInvestmentAccount_InvestmentAccountIdOrderByCreatedAtDesc(
                account.getInvestmentAccountId());
        
        return orders.stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
    }
    
    // 대기 중인 주문 조회 (PENDING)
    public List<OrderResponse> getPendingOrders(UUID userId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        List<Order> orders = orderRepository.findPendingOrdersByAccountId(account.getInvestmentAccountId());
        
        return orders.stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
    }
    
    // 계좌 ID로 대기 중인 주문 엔티티 조회 (Internal용)
    public List<Order> getPendingOrdersByAccountId(UUID accountId) {
        return orderRepository.findPendingOrdersByAccountId(accountId);
    }
    
    // 체결 완료된 주문 조회 (FILLED)
    public List<OrderResponse> getFilledOrders(UUID userId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        List<Order> orders = orderRepository.findByInvestmentAccount_InvestmentAccountIdOrderByCreatedAtDesc(
                account.getInvestmentAccountId());
        
        return orders.stream()
                .filter(order -> order.getStatus() == Order.Status.FILLED)
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
    }

    // 주문 취소
    public void cancelOrder(UUID userId, UUID orderId) {
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 주문입니다", "ORDER_NOT_FOUND"));
        
        if (!order.getInvestmentAccount().getInvestmentAccountId().equals(account.getInvestmentAccountId())) {
            throw new BusinessException("권한이 없는 주문입니다", "UNAUTHORIZED_ORDER");
        }
        
        if (order.getStatus() != Order.Status.PENDING) {
            throw new BusinessException("취소할 수 없는 주문입니다", "ORDER_NOT_CANCELLABLE");
        }
        
        order.setStatus(Order.Status.CANCELLED);
        orderRepository.save(order);
        
        log.info("주문이 취소되었습니다. 사용자: {}, 주문ID: {}", userId, orderId);
    }

    // 헬퍼 메서드들
    private InvestmentAccount getInvestmentAccountByUserId(UUID userId) {
        return investmentAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("투자 계좌를 찾을 수 없습니다.", "ACCOUNT_NOT_FOUND"));
    }

    // OrderResponse 변환
    private OrderResponse convertToOrderResponse(Order order) {
        // Stock 정보 조회
        Stock stock = order.getStock();
        
        return new OrderResponse(
                order.getOrderId(),
                order.getStock().getId(),
                stock.getStockCode() != null ? stock.getStockCode() : "",
                stock.getStockName() != null ? stock.getStockName() : "",
                order.getOrderType().toString(),
                order.getQuantity(),
                order.getPrice(),
                order.getStatus().toString(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    // BuyRequest 또는 SellRequest에서 Stock 조회
    private Stock getStockFromRequest(Object request) {
        if (request instanceof BuyRequest buyRequest) {
            if (buyRequest.getStockId() != null) {
                return stockRepository.findById(buyRequest.getStockId())
                        .orElseThrow(() -> new StockNotFoundException());
            } else if (buyRequest.getStockCode() != null) {
                return stockRepository.findByStockCode(buyRequest.getStockCode())
                        .orElseThrow(() -> new StockNotFoundException());
            } else {
                throw new BusinessException("주식 ID 또는 주식 코드가 필요합니다", "STOCK_IDENTIFIER_REQUIRED");
            }
        } else if (request instanceof SellRequest sellRequest) {
            if (sellRequest.getStockId() != null) {
                return stockRepository.findById(sellRequest.getStockId())
                        .orElseThrow(() -> new StockNotFoundException());
            } else if (sellRequest.getStockCode() != null) {
                return stockRepository.findByStockCode(sellRequest.getStockCode())
                        .orElseThrow(() -> new StockNotFoundException());
            } else {
                throw new BusinessException("주식 ID 또는 주식 코드가 필요합니다", "STOCK_IDENTIFIER_REQUIRED");
            }
        } else {
            throw new BusinessException("지원하지 않는 요청 타입입니다", "UNSUPPORTED_REQUEST_TYPE");
        }
    }

    /**
     * 거래 실패 히스토리 저장 (비동기)
     * 개인 거래와 그룹 거래를 구분하여 처리
     */
    @Async
    public void saveTradeFailedHistory(UUID userId, Stock stock, Object request, String side, String reason, UUID groupId) {
        try {
            // 수량과 가격 추출
            float quantity = 0;
            float price = 0;
            
            if (request instanceof BuyRequest buyRequest) {
                quantity = buyRequest.getQuantity();
                price = buyRequest.getPrice().floatValue();
            } else if (request instanceof SellRequest sellRequest) {
                quantity = sellRequest.getQuantity();
                price = sellRequest.getPrice().floatValue();
            }

            if (groupId != null) {
                // 그룹 거래 실패 히스토리 저장
                log.info("그룹 거래 실패 히스토리 저장 - 그룹: {}, 종목: {}, 사유: {}", 
                        groupId, stock.getStockName(), reason);
            } else {
                // 개인 거래 실패 히스토리 저장 (임시 그룹 ID 생성)
                groupId = UUID.nameUUIDFromBytes(("personal_" + userId.toString()).getBytes());
                log.info("개인 거래 실패 히스토리 저장 - 사용자: {}, 종목: {}, 사유: {}", 
                        userId, stock.getStockName(), reason);
            }

            // vote-service에 거래 실패 히스토리 저장 요청
            TradeFailedHistoryRequest historyRequest = new TradeFailedHistoryRequest(
                userId, groupId, stock.getStockName(), stock.getId(), 
                side, quantity, price, reason
            );
            voteServiceClient.saveTradeFailedHistory(historyRequest);
            
        } catch (Exception e) {
            log.error("거래 실패 히스토리 저장 실패 - 사용자: {}, 종목: {}, 사유: {} - {}", 
                    userId, stock.getStockName(), reason, e.getMessage());
        }
    }
}
