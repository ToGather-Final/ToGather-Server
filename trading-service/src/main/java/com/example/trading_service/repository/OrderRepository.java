package com.example.trading_service.repository;

import com.example.trading_service.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    // 투자 계좌별 주문 내역 조회
    List<Order> findByInvestmentAccountIdOrderByCreatedAtDesc(UUID investmentAccountId);
    
    // 특정 종목의 주문 내역 조회
    List<Order> findByInvestmentAccountIdAndStockIdOrderByCreatedAtDesc(UUID investmentAccountId, UUID stockId);
    
    // 대기 중인 주문 조회 (PENDING 상태)
    @Query("SELECT o FROM Order o WHERE o.investmentAccountId = :accountId AND o.status = 'PENDING'")
    List<Order> findPendingOrdersByAccountId(@Param("accountId") UUID accountId);
}


