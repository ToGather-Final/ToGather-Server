package com.example.trading_service.util;

import java.security.SecureRandom;
import java.util.Random;

public class AccountNumberGenerator {
    
    private static final Random random = new SecureRandom();
    
    /**
     * 실제 은행 계좌번호 형태로 생성
     * 형식: XXX-XXXX-XXXX-XX
     * 예시: 352-1488-4320-99
     */
    public static String generateAccountNumber() {
        StringBuilder accountNumber = new StringBuilder();
        
        // 첫 번째 섹션: 3자리 (100-999)
        accountNumber.append(String.format("%03d", random.nextInt(900) + 100));
        accountNumber.append("-");
        
        // 두 번째 섹션: 4자리 (1000-9999)
        accountNumber.append(String.format("%04d", random.nextInt(9000) + 1000));
        accountNumber.append("-");
        
        // 세 번째 섹션: 4자리 (1000-9999)
        accountNumber.append(String.format("%04d", random.nextInt(9000) + 1000));
        accountNumber.append("-");
        
        // 네 번째 섹션: 2자리 (10-99)
        accountNumber.append(String.format("%02d", random.nextInt(90) + 10));
        
        return accountNumber.toString();
    }
    
    /**
     * 특정 패턴의 계좌번호 생성 (테스트용)
     * 형식: 352-XXXX-XXXX-XX
     */
    public static String generateAccountNumberWithPrefix(String prefix) {
        StringBuilder accountNumber = new StringBuilder();
        
        // 고정 접두사
        accountNumber.append(prefix);
        accountNumber.append("-");
        
        // 두 번째 섹션: 4자리
        accountNumber.append(String.format("%04d", random.nextInt(9000) + 1000));
        accountNumber.append("-");
        
        // 세 번째 섹션: 4자리
        accountNumber.append(String.format("%04d", random.nextInt(9000) + 1000));
        accountNumber.append("-");
        
        // 네 번째 섹션: 2자리
        accountNumber.append(String.format("%02d", random.nextInt(90) + 10));
        
        return accountNumber.toString();
    }
    
    /**
     * 계좌번호 유효성 검증
     */
    public static boolean isValidAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return false;
        }
        
        // 형식 검증: XXX-XXXX-XXXX-XX
        String pattern = "^\\d{3}-\\d{4}-\\d{4}-\\d{2}$";
        return accountNumber.matches(pattern);
    }
    
    /**
     * 계좌번호 마스킹 (보안용)
     * 예시: 352-1488-4320-99 → 352-****-****-99
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 10) {
            return accountNumber;
        }
        
        String[] parts = accountNumber.split("-");
        if (parts.length != 4) {
            return accountNumber;
        }
        
        return parts[0] + "-****-****-" + parts[3];
    }
}




