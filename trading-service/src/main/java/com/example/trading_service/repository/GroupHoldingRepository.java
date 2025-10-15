package com.example.trading_service.repository;

import com.example.trading_service.domain.GroupHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupHoldingRepository extends JpaRepository<GroupHolding, UUID> {
    
    // 그룹별 보유 종목 조회
    List<GroupHolding> findByGroupIdOrderByUpdatedAtDesc(UUID groupId);
    
    // 특정 그룹의 특정 종목 보유 조회
    Optional<GroupHolding> findByGroupIdAndStock_Id(UUID groupId, UUID stockId);
    
    // 보유 수량이 0보다 큰 그룹 보유 종목만 조회
    List<GroupHolding> findByGroupIdAndTotalQuantityGreaterThan(UUID groupId, int quantity);
    
    // 모든 그룹의 특정 종목 보유 현황 조회
    List<GroupHolding> findByStock_IdAndTotalQuantityGreaterThan(UUID stockId, int quantity);
    
    // 그룹별 총 보유 종목 수 조회
    @Query("SELECT COUNT(gh) FROM GroupHolding gh WHERE gh.groupId = :groupId AND gh.totalQuantity > 0")
    long countByGroupIdAndTotalQuantityGreaterThan(@Param("groupId") UUID groupId);
}






