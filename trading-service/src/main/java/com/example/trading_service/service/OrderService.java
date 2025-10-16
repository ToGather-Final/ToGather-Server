package com.example.trading_service.service;

import com.example.trading_service.domain.*;
import com.example.trading_service.dto.*;
import com.example.trading_service.exception.*;
import com.example.trading_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // 주식 매수 주문
    public void buyStock(UUID userId, BuyRequest request) {
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
        
        Order savedOrder = orderRepository.save(order);

        // 시장가 주문인 경우 즉시 체결 처리
        if (request.getIsMarketOrder()) {
            tradeExecutionService.processMarketOrder(savedOrder);
        }

        log.info("매수 주문이 생성되었습니다. 사용자: {}, 종목: {}, 수량: {}, 가격: {}", 
                userId, stock.getStockName(), request.getQuantity(), request.getPrice());
    }

    // 주식 매도 주문
    public void sellStock(UUID userId, SellRequest request) {
        // 투자 계좌 조회
        InvestmentAccount account = getInvestmentAccountByUserId(userId);
        
        // 주식 정보 조회 (stockId 또는 stockCode로)
        Stock stock = getStockFromRequest(request);
        
        // 보유 종목 확인
        HoldingCache holding = holdingCacheRepository
                .findByAccountIdAndStockId(account.getInvestmentAccountId(), stock.getId())
                .orElseThrow(() -> new BusinessException("보유하지 않은 종목입니다", "HOLDING_NOT_FOUND"));

        if (holding.getQuantity() < request.getQuantity()) {
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
        
        Order savedOrder = orderRepository.save(order);

        // 시장가 주문인 경우 즉시 체결 처리
        if (request.getIsMarketOrder()) {
            tradeExecutionService.processMarketOrder(savedOrder);
        }

        log.info("매도 주문이 생성되었습니다. 사용자: {}, 종목: {}, 수량: {}, 가격: {}", 
                userId, request.getStockId(), request.getQuantity(), request.getPrice());
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
        return investmentAccountRepository.findByUserId(userId.toString())
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
                (int) order.getQuantity(),
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
}
