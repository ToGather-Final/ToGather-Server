package com.example.user_service.repository;

import com.example.user_service.domain.InvitationCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationCodeRepository extends JpaRepository<InvitationCode, String> {
    Optional<InvitationCode> findByCode(String code);

    Optional<InvitationCode> findByGroupIdAndIsExpiredFalse(UUID groupId);

    boolean existsByCode(String code);
}
