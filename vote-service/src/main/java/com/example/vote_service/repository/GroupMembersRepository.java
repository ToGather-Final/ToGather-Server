package com.example.vote_service.repository;

import com.example.vote_service.model.GroupMembers;
import com.example.vote_service.model.GroupMembersId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 그룹 멤버 리포지토리
 * - Spring Data JPA가 메서드 이름을 분석하여 자동으로 SQL 생성
 * - 직접 SQL 코드 작성 불필요
 */
@Repository
public interface GroupMembersRepository extends JpaRepository<GroupMembers, GroupMembersId> {

    /**
     * 특정 사용자가 특정 그룹의 멤버인지 확인
     * 자동 생성 SQL: SELECT COUNT(*) > 0 FROM group_members WHERE user_id = ? AND group_id = ?
     */
    boolean existsByUserIdAndGroupId(UUID userId, UUID groupId);
}
