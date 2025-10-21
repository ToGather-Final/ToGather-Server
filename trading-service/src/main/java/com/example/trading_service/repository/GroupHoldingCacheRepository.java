package com.example.trading_service.repository;

import com.example.trading_service.domain.GroupHoldingCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupHoldingCacheRepository extends JpaRepository<GroupHoldingCache, UUID> {
    
    // 그룹별 보유 종목 조회
    List<GroupHoldingCache> findByGroupIdOrderByUpdatedAtDesc(UUID groupId);
    
    // 특정 그룹의 특정 종목 보유 조회
    Optional<GroupHoldingCache> findByGroupIdAndStock_Id(UUID groupId, UUID stockId);
    
    // 보유 수량이 0보다 큰 그룹 보유 종목만 조회
    List<GroupHoldingCache> findByGroupIdAndTotalQuantityGreaterThan(UUID groupId, float quantity);

    @Query("SELECT ghc FROM GroupHoldingCache ghc JOIN FETCH ghc.stock s WHERE ghc.groupId = :groupId AND ghc.totalQuantity > :quantity ORDER BY ghc.updatedAt DESC")
    List<GroupHoldingCache> findByGroupIdAndTotalQuantityGreaterThanWithStock(@Param("groupId") UUID groupId, @Param("quantity") float quantity);
    
    // 모든 그룹의 특정 종목 보유 현황 조회
    List<GroupHoldingCache> findByStock_IdAndTotalQuantityGreaterThan(UUID stockId, float quantity);
    
    // 그룹별 총 보유 종목 수 조회
    @Query("SELECT COUNT(ghc) FROM GroupHoldingCache ghc WHERE ghc.groupId = :groupId AND ghc.totalQuantity > :quantity")
    long countByGroupIdAndTotalQuantityGreaterThan(@Param("groupId") UUID groupId, @Param("quantity") float quantity);
}
