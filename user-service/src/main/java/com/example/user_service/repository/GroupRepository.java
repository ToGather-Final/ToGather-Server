package com.example.user_service.repository;

import com.example.user_service.model.Group;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, UUID> {
    List<Group> findByOwnerId(UUID ownerId);
}
