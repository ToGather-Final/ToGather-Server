package com.example.trading_service.repository;

import com.example.trading_service.domain.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeRepository extends JpaRepository<Trade, UUID> {
    
    // 주문별 체결 내역 조회
    List<Trade> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
    
    // 투자 계좌별 체결 내역 조회 (주문을 통해)
    @Query("SELECT t FROM Trade t JOIN Order o ON t.orderId = o.orderId WHERE o.investmentAccountId = :accountId ORDER BY t.createdAt DESC")
    List<Trade> findByInvestmentAccountId(@Param("accountId") UUID accountId);
    
    // 특정 종목의 체결 내역 조회
    @Query("SELECT t FROM Trade t JOIN Order o ON t.orderId = o.orderId WHERE o.investmentAccountId = :accountId AND o.stockId = :stockId ORDER BY t.createdAt DESC")
    List<Trade> findByInvestmentAccountIdAndStockId(@Param("accountId") UUID accountId, @Param("stockId") UUID stockId);
}


