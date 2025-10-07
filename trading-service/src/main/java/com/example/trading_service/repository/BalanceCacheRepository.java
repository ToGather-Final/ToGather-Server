package com.example.trading_service.repository;

import com.example.trading_service.domain.BalanceCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BalanceCacheRepository extends JpaRepository<BalanceCache, UUID> {
    
    // 투자 계좌별 잔고 조회
    Optional<BalanceCache> findByInvestmentAccountId(UUID investmentAccountId);
}


