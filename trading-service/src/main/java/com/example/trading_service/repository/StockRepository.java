package com.example.trading_service.repository;

import com.example.trading_service.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository extends JpaRepository<Stock, UUID> {
    
    // 활성화된 주식만 조회
    List<Stock> findByEnabledTrue();
    
    // 종목 코드로 조회
    Optional<Stock> findByStockCode(String stockCode);
    
    // 종목명으로 검색 (부분 일치)
    @Query("SELECT s FROM Stock s WHERE s.enabled = true AND (s.stockName LIKE %:search% OR s.stockCode LIKE %:search%)")
    List<Stock> searchStocks(@Param("search") String search);
    
    // 국가별 주식 조회
    List<Stock> findByCountryAndEnabledTrue(Stock.Country country);
    
    // 종목 코드 존재 여부 확인
    boolean existsByStockCode(String stockCode);
}


