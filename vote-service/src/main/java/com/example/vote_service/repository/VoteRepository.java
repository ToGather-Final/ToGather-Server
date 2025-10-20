package com.example.vote_service.repository;

import com.example.vote_service.model.Vote;
import com.example.vote_service.model.VoteChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * 특정 제안의 특정 선택(AGREE/DISAGREE) 투표 수 카운트
     */
    long countByProposalIdAndChoice(UUID proposalId, VoteChoice choice);

    /**
     * 특정 사용자의 투표 존재 여부 확인
     */
    boolean existsByProposalIdAndUserId(UUID proposalId, UUID userId);

    /**
     * 여러 제안의 찬성 투표 수를 한 번에 조회 (성능 최적화)
     */
    @Query("SELECT v.proposalId, COUNT(v) FROM Vote v WHERE v.proposalId IN :proposalIds AND v.choice = 'AGREE' GROUP BY v.proposalId")
    List<Object[]> countApprovesByProposalIds(@Param("proposalIds") List<UUID> proposalIds);

    /**
     * 여러 제안의 반대 투표 수를 한 번에 조회 (성능 최적화)
     */
    @Query("SELECT v.proposalId, COUNT(v) FROM Vote v WHERE v.proposalId IN :proposalIds AND v.choice = 'DISAGREE' GROUP BY v.proposalId")
    List<Object[]> countRejectsByProposalIds(@Param("proposalIds") List<UUID> proposalIds);

    /**
     * 사용자의 여러 제안에 대한 투표를 한 번에 조회 (성능 최적화)
     */
    List<Vote> findByUserIdAndProposalIdIn(UUID userId, List<UUID> proposalIds);
}

