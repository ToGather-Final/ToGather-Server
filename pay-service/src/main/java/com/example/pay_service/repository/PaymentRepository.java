package com.example.pay_service.repository;

import com.example.pay_service.domain.Payment;
import com.example.pay_service.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByClientRequestId(String clientRequestId);

    List<Payment> findByPayerAccountIdOrderByCreatedAtDesc(UUID payerAccountId);

    Page<Payment> findByPayerAccountIdOrderByCreatedAtDesc(UUID payerAccountId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.payerAccountId = :payerAccountId " + "AND p.createdAt BETWEEN :from AND :to " + "ORDER BY p.createdAt DESC")
    Page<Payment> findByPayerAccountIdAndCreatedAtBetween(
            @Param("payerAccountId") UUID payerAccountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.merchantId = :merchantId " +
            "AND p.status = :status " +
            "ORDER BY p.createdAt DESC")
    List<Payment> findByMerchantIdAndStatusOrderByCreatedAtDesc(
            @Param("merchantId") UUID merchantId,
            @Param("status") PaymentStatus status
    );

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.payerAccountId = :payerAccountId " +
            "AND p.status = 'SUCCEEDED' " +
            "AND DATE(p.createdAt) = CURRENT_DATE")
    Long countSuccessfulPaymentsTodayByPayerAccountId(@Param("payerAccountId") UUID payerAccountId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.payerAccountId = :payerAccountId " +
            "AND p.status = 'SUCCEEDED' " +
            "AND DATE(p.createdAt) = CURRENT_DATE")
    Long sumSuccessfulPaymentsTodayByPayerAccountId(@Param("payerAccountId") UUID payerAccountId);

    Optional<Payment> findBySessionId(String sessionId);

    boolean existsByClientRequestId(String clientRequestId);
}
