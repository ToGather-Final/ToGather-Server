package com.example.user_service.support;

import com.jayway.jsonpath.JsonPath;

public class TokenSupport {
    public static String accessToken(String loginJson) {
        return JsonPath.read(loginJson, "$.accessToken");
    }

    public static String refreshToken(String loginJson) {
        return JsonPath.read(loginJson, "$.refreshToken");
    }
}
