package com.example.trading_service.service;

import com.example.trading_service.domain.*;
import com.example.trading_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchProcessingService {

    // 캐싱 테이블 Repository
    private final HoldingCacheRepository holdingCacheRepository;
    private final GroupHoldingCacheRepository groupHoldingCacheRepository;
    private final BalanceCacheRepository balanceCacheRepository;
    
    // 실제 테이블 Repository
    private final HoldingRepository holdingRepository;
    private final GroupHoldingRepository groupHoldingRepository;
    private final BalanceRepository balanceRepository;
    // private final HistoryRepository historyRepository; // 히스토리 기능 주석

    /**
     * 매일 자정에 실행되는 배치 처리
     * 캐싱 테이블의 데이터를 실제 테이블로 이동
     * 
     * 주의: @Transactional 제거 - 각 처리 메서드가 독립적인 트랜잭션을 가짐
     * 한 처리가 실패해도 다른 처리는 계속 진행됨
     */
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정
    public void processDailyBatch() {
        log.info("=== 자정 배치 처리 시작 ===");
        
        int successCount = 0;
        int failCount = 0;
        
        // 1. 개인 보유 주식 처리
        try {
            processHoldingCache();
            successCount++;
        } catch (Exception e) {
            failCount++;
            log.error("❌ 개인 보유 주식 배치 처리 실패", e);
        }
        
        // 2. 그룹 보유 주식 처리
        try {
            processGroupHoldingCache();
            successCount++;
        } catch (Exception e) {
            failCount++;
            log.error("❌ 그룹 보유 주식 배치 처리 실패", e);
        }
        
        // 3. 잔고 처리
        try {
            processBalanceCache();
            successCount++;
        } catch (Exception e) {
            failCount++;
            log.error("❌ 잔고 배치 처리 실패", e);
        }
        
        log.info("=== 자정 배치 처리 완료 - 성공: {}, 실패: {} ===", successCount, failCount);
    }

    /**
     * 개인 보유 주식 캐싱 → 실제 테이블 이동
     */
    @Transactional
    public void processHoldingCache() {
        log.info("개인 보유 주식 배치 처리 시작");
        
        List<HoldingCache> cacheHoldings = holdingCacheRepository.findAll();
        int processedCount = 0;
        
        for (HoldingCache cacheHolding : cacheHoldings) {
            try {
                // 기존 실제 테이블 데이터 조회
                Holding existingHolding = holdingRepository
                        .findByAccountIdAndStockId(
                                cacheHolding.getInvestmentAccount().getInvestmentAccountId(),
                                cacheHolding.getStock().getId())
                        .orElse(null);
                
                if (existingHolding != null) {
                    // 기존 데이터 업데이트
                    existingHolding.setQuantity(cacheHolding.getQuantity());
                    existingHolding.setAvgCost(cacheHolding.getAvgCost());
                    existingHolding.setEvaluatedPrice(cacheHolding.getEvaluatedPrice());
                    existingHolding.setUpdatedAt(LocalDateTime.now());
                    holdingRepository.save(existingHolding);
                } else {
                    // 새 데이터 생성
                    Holding newHolding = new Holding();
                    newHolding.setInvestmentAccount(cacheHolding.getInvestmentAccount());
                    newHolding.setStock(cacheHolding.getStock());
                    newHolding.setQuantity(cacheHolding.getQuantity());
                    newHolding.setAvgCost(cacheHolding.getAvgCost());
                    newHolding.setEvaluatedPrice(cacheHolding.getEvaluatedPrice());
                    newHolding.setCreatedAt(LocalDateTime.now());
                    newHolding.setUpdatedAt(LocalDateTime.now());
                    holdingRepository.save(newHolding);
                }
                
                processedCount++;
                
            } catch (Exception e) {
                log.error("개인 보유 주식 처리 실패 - 계좌ID: {}, 주식ID: {}", 
                        cacheHolding.getInvestmentAccount().getInvestmentAccountId(),
                        cacheHolding.getStock().getId(), e);
            }
        }
        
        log.info("개인 보유 주식 배치 처리 완료 - 처리된 건수: {}", processedCount);
    }

    /**
     * 그룹 보유 주식 캐싱 → 실제 테이블 이동
     */
    @Transactional
    public void processGroupHoldingCache() {
        log.info("그룹 보유 주식 배치 처리 시작");
        
        List<GroupHoldingCache> cacheGroupHoldings = groupHoldingCacheRepository.findAll();
        int processedCount = 0;
        
        for (GroupHoldingCache cacheGroupHolding : cacheGroupHoldings) {
            try {
                // 기존 실제 테이블 데이터 조회
                GroupHolding existingGroupHolding = groupHoldingRepository
                        .findByGroupIdAndStock_Id(
                                cacheGroupHolding.getGroupId(),
                                cacheGroupHolding.getStock().getId())
                        .orElse(null);
                
                if (existingGroupHolding != null) {
                    // 기존 데이터 업데이트
                    existingGroupHolding.setTotalQuantity(cacheGroupHolding.getTotalQuantity());
                    existingGroupHolding.setAvgCost(cacheGroupHolding.getAvgCost());
                    existingGroupHolding.setMemberCount(cacheGroupHolding.getMemberCount());
                    existingGroupHolding.setUpdatedAt(LocalDateTime.now());
                    groupHoldingRepository.save(existingGroupHolding);
                } else {
                    // 새 데이터 생성
                    GroupHolding newGroupHolding = new GroupHolding();
                    newGroupHolding.setGroupId(cacheGroupHolding.getGroupId());
                    newGroupHolding.setStock(cacheGroupHolding.getStock());
                    newGroupHolding.setTotalQuantity(cacheGroupHolding.getTotalQuantity());
                    newGroupHolding.setAvgCost(cacheGroupHolding.getAvgCost());
                    newGroupHolding.setMemberCount(cacheGroupHolding.getMemberCount());
                    newGroupHolding.setCreatedAt(LocalDateTime.now());
                    newGroupHolding.setUpdatedAt(LocalDateTime.now());
                    groupHoldingRepository.save(newGroupHolding);
                }
                
                processedCount++;
                
            } catch (Exception e) {
                log.error("그룹 보유 주식 처리 실패 - 그룹ID: {}, 주식ID: {}", 
                        cacheGroupHolding.getGroupId(),
                        cacheGroupHolding.getStock().getId(), e);
            }
        }
        
        log.info("그룹 보유 주식 배치 처리 완료 - 처리된 건수: {}", processedCount);
    }

    /**
     * 잔고 캐싱 → 실제 테이블 이동
     */
    @Transactional
    public void processBalanceCache() {
        log.info("잔고 배치 처리 시작");
        
        List<BalanceCache> cacheBalances = balanceCacheRepository.findAll();
        int processedCount = 0;
        int failedCount = 0;
        
        for (BalanceCache cacheBalance : cacheBalances) {
            try {
                // 투자 계좌 존재 여부 확인 (null 체크 추가)
                if (cacheBalance.getInvestmentAccount() == null) {
                    log.warn("⚠️ BalanceCache에 투자 계좌가 없음 - 스킵");
                    failedCount++;
                    continue;
                }
                
                // 기존 실제 테이블 데이터 조회
                Balance existingBalance = balanceRepository
                        .findByAccountId(
                                cacheBalance.getInvestmentAccount().getInvestmentAccountId())
                        .orElse(null);
                
                if (existingBalance != null) {
                    // 기존 데이터 업데이트
                    existingBalance.setBalance(cacheBalance.getBalance());
                    existingBalance.setUpdatedAt(LocalDateTime.now());
                    balanceRepository.save(existingBalance);
                    processedCount++;
                } else {
                    // 새 데이터 생성 (investment_account가 실제로 존재하는지 확인 불가능하므로 시도)
                    // FK 제약 조건 위반 시 catch로 처리
                    Balance newBalance = new Balance();
                    newBalance.setInvestmentAccount(cacheBalance.getInvestmentAccount());
                    newBalance.setBalance(cacheBalance.getBalance());
                    newBalance.setCreatedAt(LocalDateTime.now());
                    newBalance.setUpdatedAt(LocalDateTime.now());
                    balanceRepository.save(newBalance);
                    processedCount++;
                }
                
            } catch (Exception e) {
                failedCount++;
                log.error("❌ 잔고 처리 실패 - 계좌ID: {} (존재하지 않는 계좌일 수 있음. BalanceCache 데이터 확인 필요)", 
                        cacheBalance.getInvestmentAccount() != null ? 
                        cacheBalance.getInvestmentAccount().getInvestmentAccountId() : "NULL", e);
                // FK 제약 조건 위반 시 해당 BalanceCache는 스킵하고 계속 진행
            }
        }
        
        log.info("잔고 배치 처리 완료 - 성공: {}, 실패: {}, 전체: {}", 
                processedCount, failedCount, cacheBalances.size());
    }

    /**
     * 수동 배치 처리 실행 (테스트용)
     */
    @Transactional
    public void executeManualBatch() {
        log.info("수동 배치 처리 시작");
        processDailyBatch();
        log.info("수동 배치 처리 완료");
    }

    /**
     * 캐싱 테이블 초기화 (배치 처리 후)
     */
    @Transactional
    public void clearCacheTables() {
        log.info("캐싱 테이블 초기화 시작");
        
        try {
            holdingCacheRepository.deleteAll();
            groupHoldingCacheRepository.deleteAll();
            balanceCacheRepository.deleteAll();
            
            log.info("캐싱 테이블 초기화 완료");
        } catch (Exception e) {
            log.error("캐싱 테이블 초기화 실패", e);
            throw e;
        }
    }
}




