package com.example.user_service.repository;

import com.example.user_service.model.InvitationCode;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationCodeRepository extends JpaRepository<InvitationCode, UUID> {
}
