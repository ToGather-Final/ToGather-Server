package com.example.pay_service.repository;

import com.example.pay_service.domain.Account;
import com.example.pay_service.domain.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByOwnerUserIdAndIsActiveTrue(UUID ownerUserId);

    List<Account> findByOwnerUserIdAndTypeAndIsActiveTrue(UUID ownerUserId, AccountType type);

    Optional<Account> findByIdAndIsActiveTrue(UUID id);

    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.ownerUserId = :ownerUserId AND a.isActive = true")
    Optional<Account> findByIdAndOwnerUserIdAndIsActiveTrue(@Param("id") UUID id, @Param("ownerUserId") UUID ownerUserId);

    @Query("SELECT a FROM Account a WHERE a.type = :type AND a.isActive = true")
    List<Account> findByTypeAndIsActiveTrue(@Param("type") AccountType type);

    @Query("SELECT a FROM Account a WHERE a.groupId = :groupId AND a.type = 'GROUP_PAY' AND a.isActive = true")
    Optional<Account> findGroupPayAccountByGroupId(@Param("groupId") UUID groupId);

    boolean existsByIdAndOwnerUserIdAndIsActiveTrue(UUID id, UUID ownerUserId);

    boolean existsByOwnerUserIdAndTypeAndIsActiveTrue(UUID ownerUserId, AccountType type);
}
