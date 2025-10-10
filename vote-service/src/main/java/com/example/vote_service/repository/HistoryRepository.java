package com.example.vote_service.repository;

import com.example.vote_service.model.History;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * History Repository
 * - 히스토리 데이터 저장을 위한 JPA Repository
 */
@Repository
public interface HistoryRepository extends JpaRepository<History, UUID> {
}
