package com.example.vote_service.repository;

import com.example.vote_service.model.Proposal;
import com.example.vote_service.model.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Proposal Repository
 * - 제안 데이터 접근 인터페이스
 */
@Repository
public interface ProposalRepository extends JpaRepository<Proposal, UUID> {

    /**
     * 특정 그룹의 모든 제안 조회
     */
    List<Proposal> findByGroupId(UUID groupId);

    /**
     * 특정 그룹의 특정 상태의 제안 조회
     */
    List<Proposal> findByGroupIdAndStatus(UUID groupId, ProposalStatus status);

    /**
     * 특정 사용자가 생성한 제안 조회
     */
    List<Proposal> findByUserId(UUID userId);

    /**
     * 특정 상태의 모든 제안 조회 (스케줄러용)
     */
    List<Proposal> findByStatus(ProposalStatus status);
}

