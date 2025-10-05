package com.example.vote_service.repository;

import com.example.vote_service.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Vote Repository
 * - 투표 데이터 접근 인터페이스
 */
@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {

    /**
     * 특정 제안에 대한 모든 투표 조회
     */
    List<Vote> findByProposalId(UUID proposalId);

    /**
     * 특정 사용자가 특정 제안에 투표했는지 조회
     */
    Optional<Vote> findByProposalIdAndUserId(UUID proposalId, UUID userId);

    /**
     * 특정 제안의 투표 수 카운트
     */
    long countByProposalId(UUID proposalId);

    /**
     * 특정 제안의 찬성 투표 수 카운트
     */
    long countByProposalIdAndChoice(UUID proposalId, Boolean choice);

    /**
     * 특정 사용자의 투표 존재 여부 확인
     */
    boolean existsByProposalIdAndUserId(UUID proposalId, UUID userId);
}

