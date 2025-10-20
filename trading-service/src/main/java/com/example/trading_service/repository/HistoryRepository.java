package com.example.trading_service.repository;

import com.example.trading_service.domain.History;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface HistoryRepository extends JpaRepository<History, UUID> {
    
    // 투자 계좌별 거래 히스토리 조회
    List<History> findByInvestmentAccount_InvestmentAccountIdOrderByCreatedAtDesc(UUID investmentAccountId);
    
    // 특정 종목의 거래 히스토리 조회
    List<History> findByInvestmentAccount_InvestmentAccountIdAndStock_IdOrderByCreatedAtDesc(
            UUID investmentAccountId, UUID stockId);
    
    // 그룹 거래 히스토리 조회
    List<History> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
    
    // 특정 기간 내 거래 히스토리 조회
    @Query("SELECT h FROM History h WHERE h.investmentAccount.investmentAccountId = :accountId " +
           "AND h.createdAt BETWEEN :startDate AND :endDate ORDER BY h.createdAt DESC")
    List<History> findByInvestmentAccountIdAndDateRange(
            @Param("accountId") UUID accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    // 거래 유형별 히스토리 조회
    List<History> findByInvestmentAccount_InvestmentAccountIdAndTransactionTypeOrderByCreatedAtDesc(
            UUID investmentAccountId, History.TransactionType transactionType);
}






