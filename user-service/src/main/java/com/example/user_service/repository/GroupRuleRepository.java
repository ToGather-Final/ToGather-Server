package com.example.user_service.repository;

import com.example.user_service.domain.GroupRule;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRuleRepository extends JpaRepository<GroupRule, UUID> {
    Optional<GroupRule> findByGroupId(UUID groupId);
}
