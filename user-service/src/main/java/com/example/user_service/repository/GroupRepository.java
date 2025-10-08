package com.example.user_service.repository;

import com.example.user_service.model.Group;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupRepository extends JpaRepository<Group, UUID> {
    List<Group> findByOwnerId(UUID ownerId);

    @Query("select g from Group g where g.groupId in "
            + "(select gm.id.groupId from GroupMember gm where gm.id.userId=:userId)")
    List<Group> findAllByMember(@Param("userId") UUID userId);
}
