package com.example.vote_service.model;

/**
 * 히스토리 타입
 * - 투표 관련: VOTE_CREATED_BUY, VOTE_CREATED_SELL, VOTE_CREATED_PAY, VOTE_APPROVED, VOTE_REJECTED
 * - 매매 관련: TRADE_EXECUTED, TRADE_FAILED
 * - 예수금 관련: CASH_DEPOSIT_COMPLETED
 * - 페이 관련: PAY_CHARGE_COMPLETED
 * - 목표 관련: GOAL_ACHIEVED
 */
public enum HistoryType {
    // 투표 관련
    VOTE_CREATED_BUY,       // 매수 투표 올라옴
    VOTE_CREATED_SELL,      // 매도 투표 올라옴
    VOTE_CREATED_PAY,       // 페이 관련 투표 올라옴 (예수금/페이 충전)
    VOTE_APPROVED,          // 투표 가결
    VOTE_REJECTED,          // 투표 부결
    
    // 매매 관련
    TRADE_EXECUTED,         // 매도/매수 완료
    TRADE_FAILED,           // 매도/매수 실패
    
    // 예수금 관련
    CASH_DEPOSIT_COMPLETED, // 예수금 충전 완료
    
    // 페이 관련
    PAY_CHARGE_COMPLETED,   // 페이 충전 완료(모임통장으로 송금)
    
    // 목표 관련
    GOAL_ACHIEVED           // 목표 달성
}
