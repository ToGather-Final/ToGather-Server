package com.example.trading_service.repository;

import com.example.trading_service.domain.CashTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CashTransactionRepository extends JpaRepository<CashTransaction, UUID> {
    
    // 투자 계좌별 현금 거래 내역 조회
    List<CashTransaction> findByInvestmentAccount_InvestmentAccountIdOrderByCreatedAtDesc(UUID investmentAccountId);
    
    // 특정 기간 내 현금 거래 내역 조회
    @Query("SELECT ct FROM CashTransaction ct WHERE ct.investmentAccount.investmentAccountId = :accountId " +
           "AND ct.createdAt BETWEEN :startDate AND :endDate ORDER BY ct.createdAt DESC")
    List<CashTransaction> findByInvestmentAccountIdAndDateRange(
            @Param("accountId") UUID accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    // 거래 유형별 현금 거래 내역 조회
    List<CashTransaction> findByInvestmentAccount_InvestmentAccountIdAndTransactionTypeOrderByCreatedAtDesc(
            UUID investmentAccountId, CashTransaction.TransactionType transactionType);
}






