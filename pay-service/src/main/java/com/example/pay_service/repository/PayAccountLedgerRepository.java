package com.example.pay_service.repository;

import com.example.pay_service.domain.PayAccountLedger;
import com.example.pay_service.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PayAccountLedgerRepository extends JpaRepository<PayAccountLedger, Long> {

    // 계좌별 원장 조회 (최신순)
    List<PayAccountLedger> findByPayAccountIdOrderByCreatedAtDesc(UUID payAccountId);

    // 계좌별 거래 타입별 원장 조회
    List<PayAccountLedger> findByPayAccountIdAndTransactionTypeOrderByCreatedAtDesc(
            UUID payAccountId, TransactionType transactionType);

    // 기간별 원장 조회
    @Query("SELECT pal FROM PayAccountLedger pal WHERE pal.payAccountId = :payAccountId " +
            "AND pal.createdAt BETWEEN :startDate AND :endDate ORDER BY pal.createdAt DESC")
    List<PayAccountLedger> findByPayAccountIdAndCreatedAtBetween(
            @Param("payAccountId") UUID payAccountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 결제 관련 원장 조회
    List<PayAccountLedger> findByRelatedPaymentIdOrderByCreatedAtDesc(UUID relatedPaymentId);

    // 이체 관련 원장 조회
    List<PayAccountLedger> findByRelatedTransferIdOrderByCreatedAtDesc(UUID relatedTransferId);

    // 계좌별 최신 잔액 조회 (가장 최근 원장의 balance_after)
    @Query("SELECT pal.balanceAfter FROM PayAccountLedger pal " +
            "WHERE pal.payAccountId = :payAccountId " +
            "ORDER BY pal.createdAt DESC LIMIT 1")
    Long findLatestBalanceByPayAccountId(@Param("payAccountId") UUID payAccountId);

    // 계좌별 거래 건수 조회
    @Query("SELECT COUNT(pal) FROM PayAccountLedger pal WHERE pal.payAccountId = :payAccountId")
    Long countByPayAccountId(@Param("payAccountId") UUID payAccountId);

    // 계좌별 특정 타입 거래 건수 조회
    @Query("SELECT COUNT(pal) FROM PayAccountLedger pal " +
            "WHERE pal.payAccountId = :payAccountId AND pal.transactionType = :transactionType")
    Long countByPayAccountIdAndTransactionType(
            @Param("payAccountId") UUID payAccountId,
            @Param("transactionType") TransactionType transactionType);

    // 계좌별 총 입금액 조회
    @Query("SELECT SUM(pal.amount) FROM PayAccountLedger pal " +
            "WHERE pal.payAccountId = :payAccountId AND pal.transactionType IN ('TRANSFER_IN', 'CHARGE')")
    Long getTotalDepositAmount(@Param("payAccountId") UUID payAccountId);

    // 계좌별 총 출금액 조회
    @Query("SELECT SUM(pal.amount) FROM PayAccountLedger pal " +
            "WHERE pal.payAccountId = :payAccountId AND pal.transactionType IN ('TRANSFER_OUT', 'PAYMENT')")
    Long getTotalWithdrawalAmount(@Param("payAccountId") UUID payAccountId);

    // 그룹별 원장 조회 (PayAccount를 통해)
    @Query("SELECT pal FROM PayAccountLedger pal " +
            "JOIN PayAccount pa ON pal.payAccountId = pa.id " +
            "WHERE pa.groupId = :groupId ORDER BY pal.createdAt DESC")
    List<PayAccountLedger> findByGroupId(@Param("groupId") UUID groupId);

    // 특정 기간 내 거래 내역 조회
    @Query("SELECT pal FROM PayAccountLedger pal " +
            "WHERE pal.createdAt >= :startDate AND pal.createdAt <= :endDate " +
            "ORDER BY pal.createdAt DESC")
    List<PayAccountLedger> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}