package com.example.user_service.repository;

import com.example.user_service.model.GroupMember;
import com.example.user_service.model.GroupMemberId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    List<GroupMember> findByIdGroupId(UUID groupId);

    Optional<GroupMember> findByIdGroupIdAndIdUserId(UUID groupId, UUID userId);

    boolean existsByIdGroupIdAndIdUserId(UUID groupId, UUID userId);
}
