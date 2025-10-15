package com.example.pay_service.repository;

import com.example.pay_service.domain.Transfer;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    Optional<Transfer> findByClientRequestId(String clientRequestId);

    @Query("SELECT t FROM Transfer t WHERE t.fromAccountId = :accountId OR t.toAccountId = :accountId " +
            "ORDER BY t.createdAt DESC")
    List<Transfer> findByAccountIdOrderByCreatedAtDesc(@Param("accountId") UUID accountId);

    @Query("SELECT t FROM Transfer t WHERE (t.fromAccountId = :accountId OR t.toAccountId = :accountId) " +
            "AND t.createdAt BETWEEN :from AND :to " +
            "ORDER BY t.createdAt DESC")
    List<Transfer> findByAccountIdAndCreatedAtBetween(
            @Param("accountId") UUID accountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transfer t WHERE t.clientRequestId = :clientRequestId")
    Optional<Transfer> findByClientRequestIdWithLock(@Param("clientRequestId") String clientRequestId);

    Page<Transfer> findByToAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);
}
