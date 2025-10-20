package com.example.trading_service.repository;

import com.example.trading_service.domain.BalanceCache;
import com.example.trading_service.domain.InvestmentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BalanceCacheRepository extends JpaRepository<BalanceCache, UUID> {
    
    // 투자 계좌별 잔고 조회
    @Query("SELECT b FROM BalanceCache b WHERE b.investmentAccount.investmentAccountId = :accountId")
    Optional<BalanceCache> findByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT b FROM BalanceCache b WHERE b.investmentAccount = :investmentAccount")
    Optional<BalanceCache> findByInvestmentAccount(@Param("investmentAccount") InvestmentAccount investmentAccount);

}


