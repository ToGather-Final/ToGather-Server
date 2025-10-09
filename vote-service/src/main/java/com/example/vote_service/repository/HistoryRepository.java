package com.example.vote_service.repository;

import com.example.vote_service.model.History;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * History Repository
 * - 히스토리 데이터 접근을 위한 JPA Repository
 */
@Repository
public interface HistoryRepository extends JpaRepository<History, UUID> {

    /**
     * 특정 그룹의 히스토리 조회 (최신순)
     */
    List<History> findByGroupIdOrderByCreatedAtDesc(UUID groupId);

    /**
     * 특정 그룹의 특정 카테고리 히스토리 조회
     */
    List<History> findByGroupIdAndHistoryCategoryOrderByCreatedAtDesc(UUID groupId, String historyCategory);

    /**
     * 특정 그룹의 특정 타입 히스토리 조회
     */
    List<History> findByGroupIdAndHistoryTypeOrderByCreatedAtDesc(UUID groupId, String historyType);
}
