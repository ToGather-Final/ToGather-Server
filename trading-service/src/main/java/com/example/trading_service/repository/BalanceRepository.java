package com.example.trading_service.repository;

import com.example.trading_service.domain.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BalanceRepository extends JpaRepository<Balance, UUID> {
    
    // 투자 계좌별 잔고 조회
    Optional<Balance> findByInvestmentAccount_InvestmentAccountId(UUID investmentAccountId);
}






