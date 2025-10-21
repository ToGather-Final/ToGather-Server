package com.example.pay_service.util;

import java.security.SecureRandom;
import java.util.Random;

public class PayAccountNumberGenerator {

    private static final Random random = new SecureRandom();

    public static String generatePayAccountNumber() {
        StringBuilder accountNumber = new StringBuilder();

        // 첫 번째 3자리 (100-999)
        accountNumber.append(String.format("%03d", random.nextInt(900) + 100));
        accountNumber.append("-");

        // 두 번째 3자리 (100-999)
        accountNumber.append(String.format("%03d", random.nextInt(900) + 100));
        accountNumber.append("-");

        // 세 번째 6자리 (100000-999999)
        accountNumber.append(String.format("%06d", random.nextInt(900000) + 100000));

        return accountNumber.toString();
    }
}
