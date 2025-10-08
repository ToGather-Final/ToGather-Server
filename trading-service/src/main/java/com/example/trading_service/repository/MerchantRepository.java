package com.example.trading_service.repository;

import com.example.trading_service.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    List<Merchant> findByIsActiveTrue();

    Optional<Merchant> findByIdAndIsActiveTrue(UUID id);

    @Query("SELECT m FROM Merchant m WHERE m.displayName LIKE %:displayName% AND m.isActive = true")
    List<Merchant> findByDisplayNameContainingAndIsActiveTrue(@Param("displayName") String displayName);

    @Query("SELECT m FROM Merchant m WHERE m.settlementAccountId = :settlementAccountId AND m.isActive = true")
    Optional<Merchant> findBySettlementAccountIdAndIsActiveTrue(UUID settlementAccountId);

    boolean existsByIdAndIsActiveTrue(UUID id);
}
