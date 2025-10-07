package com.example.user_service.support;

import java.util.Map;

public class TestFixtures {
    public static Map<String, String> signupBody(String e, String p, String n) {
        return Map.of("email", e, "password", p, "nickname", n);
    }

    public static Map<String, String> loginBody(String e, String p) {
        return Map.of("email", e, "password", p);
    }

    public static Map<String, String> nicknameBody(String n) {
        return Map.of("nickname", n);
    }
}
