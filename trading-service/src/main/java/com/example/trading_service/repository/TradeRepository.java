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
    List<Trade> findByOrder_OrderIdOrderByCreatedAtDesc(UUID orderId);
    
    // 투자 계좌별 체결 내역 조회 (주문을 통해)
    @Query("SELECT t FROM Trade t JOIN t.order o WHERE o.investmentAccount.investmentAccountId = :accountId ORDER BY t.createdAt DESC")
    List<Trade> findByInvestmentAccountId(@Param("accountId") UUID accountId);
    
    // 특정 종목의 체결 내역 조회
    @Query("SELECT t FROM Trade t JOIN t.order o WHERE o.investmentAccount.investmentAccountId = :accountId AND o.stock.id = :stockId ORDER BY t.createdAt DESC")
    List<Trade> findByInvestmentAccountIdAndStockId(@Param("accountId") UUID accountId, @Param("stockId") UUID stockId);

    @Query("SELECT t FROM Trade t JOIN FETCH t.order o JOIN FETCH o.stock s WHERE o.investmentAccount.investmentAccountId = :accountId ORDER BY t.createdAt DESC")
    List<Trade> findByInvestmentAccountIdWithOrderAndStock(@Param("accountId") UUID accountId);

    @Query("SELECT t FROM Trade t JOIN FETCH t.order o JOIN FETCH o.stock s WHERE o.investmentAccount.investmentAccountId = :accountId AND o.stock.id = :stockId ORDER BY t.createdAt DESC")
    List<Trade> findByInvestmentAccountIdAndStockIdWithOrderAndStock(@Param("accountId") UUID accountId, @Param("stockId") UUID stockId);
}


