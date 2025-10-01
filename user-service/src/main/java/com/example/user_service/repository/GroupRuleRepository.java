package com.example.user_service.repository;

import com.example.user_service.model.GroupRule;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRuleRepository extends JpaRepository<GroupRule, UUID> {
}
