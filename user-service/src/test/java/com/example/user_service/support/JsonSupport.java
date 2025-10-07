package com.example.user_service.support;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonSupport {
    private static final ObjectMapper om = new ObjectMapper();

    public static String toJson(Object o) {
        try {
            return om.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
