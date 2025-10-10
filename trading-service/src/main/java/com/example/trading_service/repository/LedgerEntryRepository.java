package com.example.trading_service.repository;

import com.example.trading_service.domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    @Query("SELECT l FROM LedgerEntry l WHERE (l.debitAccountId = :accountId OR l.creditAccountId = :accountId) " +
            "ORDER BY l.createdAt DESC")
    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(@Param("accountId") UUID accountId);

    @Query("SELECT l FROM LedgerEntry l WHERE (l.debitAccountId = :accountId OR l.creditAccountId = :accountId) " +
            "ORDER BY l.createdAt DESC")
    Page<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(@Param("accountId") UUID accountId, Pageable pageable);

    @Query("SELECT l FROM LedgerEntry l WHERE (l.debitAccountId = :accountId OR l.creditAccountId = :accountId) " +
            "AND l.createdAt BETWEEN :from AND :to " +
            "ORDER BY l.createdAt DESC")
    Page<LedgerEntry> findByAccountIdAndCreatedAtBetween(
            @Param("accountId") UUID accountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    List<LedgerEntry> findByTxId(UUID txId);

    List<LedgerEntry> findByRelatedPaymentId(UUID paymentId);

    @Query("SELECT l FROM LedgerEntry l WHERE l.status = :status " +
            "AND l.createdAt BETWEEN :from AND :to")
    List<LedgerEntry> findByStatusAndCreatedAtBetween(
            @Param("status") TransactionStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT SUM(l.amount) FROM LedgerEntry l WHERE l.debitAccountId = :accountId " +
            "AND l.status = 'COMPLETED' " +
            "AND DATE(l.createdAt) = CURRENT_DATE")
    Long sumDebitAmountTodayByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT SUM(l.amount) FROM LedgerEntry l WHERE l.creditAccountId = :accountId " +
            "AND l.status = 'COMPLETED' " +
            "AND DATE(l.createdAt) = CURRENT_DATE")
    Long sumCreditAmountTodayByAccountId(@Param("accountId") UUID accountId);

    boolean existsByTxId(UUID txId);
}
