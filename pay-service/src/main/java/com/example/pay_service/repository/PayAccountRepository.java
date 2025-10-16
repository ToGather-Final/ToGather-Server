package com.example.pay_service.repository;

import com.example.pay_service.domain.PayAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayAccountRepository extends JpaRepository<PayAccount, UUID> {

    List<PayAccount> findByOwnerUserIdAndIsActiveTrue(UUID ownerUserId);

    Optional<PayAccount> findByIdAndIsActiveTrue(UUID id);

    @Query("SELECT pa FROM PayAccount pa WHERE pa.id = :id AND pa.ownerUserId = :ownerUserId AND pa.isActive = true")
    Optional<PayAccount> findByIdAndOwnerUserIdAndIsActiveTrue(@Param("id") UUID id, @Param("ownerUserId") UUID ownerUserId);

    @Query("SELECT pa FROM PayAccount pa WHERE pa.groupId = :groupId AND pa.isActive = true")
    Optional<PayAccount> findGroupPayAccountByGroupId(@Param("groupId") UUID groupId);

    boolean existsByIdAndOwnerUserIdAndIsActiveTrue(UUID id, UUID ownerUserId);

    boolean existsByGroupIdAndIsActiveTrue(UUID groupId);

    boolean existsByOwnerUserIdAndIsActiveTrue(UUID ownerUserId);

    List<PayAccount> findByGroupIdAndIsActiveTrue(UUID groupId);

    @Query("SELECT pa FROM PayAccount pa WHERE pa.nickname = :nickname AND pa.ownerUserId = :ownerUserId AND pa.isActive = true")
    Optional<PayAccount> findByNicknameAndOwnerUserIdAndIsActiveTrue(@Param("nickname") String nickname, @Param("ownerUserId") UUID ownerUserId);
}
