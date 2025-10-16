package com.example.vote_service.repository;

import com.example.vote_service.model.History;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * History Repository
 * - 히스토리 데이터 저장을 위한 JPA Repository
 */
@Repository
public interface HistoryRepository extends JpaRepository<History, UUID> {

    /**
     * 특정 그룹의 히스토리 조회 (최신순)
     */
    Page<History> findByGroupIdOrderByCreatedAtDesc(UUID groupId, Pageable pageable);

    /**
     * 특정 그룹의 히스토리 조회 (최신순, 페이징 없이)
     */
    List<History> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
}
