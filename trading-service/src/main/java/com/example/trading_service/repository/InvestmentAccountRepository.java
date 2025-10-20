package com.example.trading_service.repository;

import com.example.trading_service.domain.InvestmentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvestmentAccountRepository extends JpaRepository<InvestmentAccount, UUID> {
    
    // 사용자별 투자 계좌 조회
    Optional<InvestmentAccount> findByUserId(UUID userId);
    
    // 계좌번호로 계좌 조회
    Optional<InvestmentAccount> findByAccountNo(String accountNo);
    
    // 사용자별 계좌 존재 여부 확인
    boolean existsByUserId(UUID userId);
}


