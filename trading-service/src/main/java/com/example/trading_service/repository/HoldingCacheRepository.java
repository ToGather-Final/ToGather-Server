package com.example.trading_service.repository;

import com.example.trading_service.domain.HoldingCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HoldingCacheRepository extends JpaRepository<HoldingCache, UUID> {
    
    // 투자 계좌별 보유 종목 조회
    List<HoldingCache> findByInvestmentAccount_InvestmentAccountId(UUID investmentAccountId);
    
    // 특정 계좌의 특정 종목 보유 조회
    Optional<HoldingCache> findByInvestmentAccount_InvestmentAccountIdAndStock_Id(UUID investmentAccountId, UUID stockId);
    
    // 보유 수량이 0보다 큰 종목만 조회
    List<HoldingCache> findByInvestmentAccount_InvestmentAccountIdAndQuantityGreaterThan(UUID investmentAccountId, int quantity);
}


