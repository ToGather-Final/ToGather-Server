package com.example.trading_service.service;

import com.example.trading_service.dto.GroupPortfolioResponse;
import com.example.trading_service.dto.GroupGoalStatusResponse;
import com.example.trading_service.repository.GroupHoldingCacheRepository;
import com.example.trading_service.repository.BalanceCacheRepository;
import com.example.trading_service.repository.InvestmentAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GroupPortfolioService {

    private final GroupHoldingCacheRepository groupHoldingCacheRepository;
    private final BalanceCacheRepository balanceCacheRepository;
    private final InvestmentAccountRepository investmentAccountRepository;

    /**
     * 그룹 포트폴리오 조회 (홈 화면용)
     */
    public GroupPortfolioResponse getGroupPortfolio(UUID groupId) {
        log.info("그룹 포트폴리오 조회 - 그룹 ID: {}", groupId);
        
        // TODO: 실제 그룹 정보 조회 로직 구현
        // 현재는 임시 데이터 반환
        
        return GroupPortfolioResponse.builder()
                .groupId(groupId.toString())
                .groupName("테스트 그룹")
                .memberCount(5)
                .totalValue(BigDecimal.valueOf(10000000)) // 1천만원
                .totalInvestment(BigDecimal.valueOf(8000000)) // 8백만원
                .totalProfit(BigDecimal.valueOf(2000000)) // 2백만원
                .profitRate(BigDecimal.valueOf(25.0)) // 25%
                .holdings(List.of()) // TODO: 실제 보유 종목 조회
                .goalAmount(BigDecimal.valueOf(15000000)) // 1천5백만원
                .goalProgress(BigDecimal.valueOf(66.67)) // 66.67%
                .build();
    }

    /**
     * 그룹 목표 달성 상태 확인
     */
    public GroupGoalStatusResponse getGroupGoalStatus(UUID groupId) {
        log.info("그룹 목표 달성 상태 확인 - 그룹 ID: {}", groupId);
        
        // TODO: 실제 그룹 목표 및 현재 금액 조회 로직 구현
        // 현재는 임시 데이터 반환
        
        BigDecimal goalAmount = BigDecimal.valueOf(15000000); // 1천5백만원
        BigDecimal currentAmount = BigDecimal.valueOf(10000000); // 1천만원
        BigDecimal progressRate = currentAmount.divide(goalAmount, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        BigDecimal remainingAmount = goalAmount.subtract(currentAmount);
        boolean isGoalAchieved = currentAmount.compareTo(goalAmount) >= 0;
        
        String statusMessage;
        if (isGoalAchieved) {
            statusMessage = "목표를 달성했습니다! 🎉";
        } else {
            statusMessage = String.format("목표까지 %.2f%% 남았습니다", 
                    goalAmount.subtract(currentAmount).divide(goalAmount, 4, BigDecimal.ROUND_HALF_UP)
                            .multiply(BigDecimal.valueOf(100)));
        }
        
        return GroupGoalStatusResponse.builder()
                .groupId(groupId.toString())
                .groupName("테스트 그룹")
                .goalAmount(goalAmount)
                .currentAmount(currentAmount)
                .progressRate(progressRate)
                .remainingAmount(remainingAmount)
                .isGoalAchieved(isGoalAchieved)
                .statusMessage(statusMessage)
                .build();
    }
}
