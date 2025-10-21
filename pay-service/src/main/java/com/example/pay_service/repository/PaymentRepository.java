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

    List<Payment> findByPayerAccountIdOrderByCreatedAtDesc(UUID payerAccountId);

    Optional<Payment> findByClientRequestId(String clientRequestId);

    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<Payment> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p FROM Payment p WHERE p.recipientBankCode = :bankCode AND p.recipientAccountNumber = :accountNumber ORDER BY p.createdAt DESC")
    List<Payment> findByRecipientAccount(@Param("bankCode") String bankCode, @Param("accountNumber") String accountNumber);

    @Query("SELECT p FROM Payment p JOIN PayAccount pa ON p.payerAccountId = pa.id WHERE pa.groupId = :groupId ORDER BY p.createdAt DESC")
    List<Payment> findByGroupId(@Param("groupId") UUID groupId);

    Page<Payment> findByPayerAccountIdOrderByCreatedAtDesc(UUID payerAccountId, Pageable pageable);
}
