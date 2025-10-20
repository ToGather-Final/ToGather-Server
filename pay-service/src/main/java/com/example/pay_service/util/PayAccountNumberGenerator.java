package com.example.pay_service.util;

import java.security.SecureRandom;
import java.util.Random;

public class PayAccountNumberGenerator {

    private static final Random random = new SecureRandom();

    public static String generatePayAccountNumber() {
        StringBuilder accountNumber = new StringBuilder("PAY-");

        accountNumber.append(String.format("%04d", random.nextInt(9000) + 1000));
        accountNumber.append("-");

        accountNumber.append(String.format("%04d", random.nextInt(9000) + 1000));
        accountNumber.append("-");

        accountNumber.append(String.format("%04d", random.nextInt(9000) + 1000));

        return accountNumber.toString();
    }
}
