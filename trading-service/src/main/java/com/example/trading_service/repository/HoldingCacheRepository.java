package com.example.trading_service.repository;

import com.example.trading_service.domain.HoldingCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HoldingCacheRepository extends JpaRepository<HoldingCache, UUID> {
    
    // 투자 계좌별 보유 종목 조회
    @Query("SELECT h FROM HoldingCache h WHERE h.investmentAccount.investmentAccountId = :accountId")
    List<HoldingCache> findByAccountId(@Param("accountId") UUID accountId);
    
    // 특정 계좌의 특정 종목 보유 조회
    @Query("SELECT h FROM HoldingCache h WHERE h.investmentAccount.investmentAccountId = :accountId AND h.stock.id = :stockId")
    Optional<HoldingCache> findByAccountIdAndStockId(@Param("accountId") UUID accountId, @Param("stockId") UUID stockId);
    
    // 보유 수량이 0보다 큰 종목만 조회
    @Query("SELECT h FROM HoldingCache h WHERE h.investmentAccount.investmentAccountId = :accountId AND h.quantity > :quantity")
    List<HoldingCache> findByAccountIdAndQuantityGreaterThan(@Param("accountId") UUID accountId, @Param("quantity") float quantity);
}


