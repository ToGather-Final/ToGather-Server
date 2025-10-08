package com.example.trading_service.repository;

import com.example.trading_service.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByIdempotencyKeyAndPayerAccountId(String idempotencyKey, UUID payerAccountId);

    boolean existsIdempotencyKeyAndPayerAccountId(String idempotencyKey, UUID payerAccountId);

    @Query("SELECT ik FROM IdempotencyKey ik WHERE ik.idempotencyKey = :idempotencyKey " +
            "AND ik.payerAccountId = :payerAccountId " +
            "AND ik.isUsed = true")
    Optional<IdempotencyKey> findUsedByIdempotencyKeyAndPayerAccountId(
            @Param("idempotencyKey") String idempotencyKey,
            @Param("payerAccountId") UUID payerAccountId
    );

    List<IdempotencyKey> findByPayerAccountIdOrderByCreatedAtDesc(UUID payerAccountId);

    List<IdempotencyKey> findByIsUsedTrue();

    boolean existsByIdempotencyKeyAndPayerAccountIdAndIsUsedTrue(String idempotencyKey, UUID payerAccountId);
}
