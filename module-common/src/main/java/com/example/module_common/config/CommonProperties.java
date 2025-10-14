package com.example.module_common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Data
public class CommonProperties {
    private Database database = new Database();
    private Redis redis = new Redis();
    private Jwt jwt = new Jwt();

    @Data
    public static class Database {
        private String url = "jdbc:mysql://localhost:3306/togather_db";
        private String username = "admin";
        private String password = "togather1234";
    }

    @Data
    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
        private String password = "togather1234";
    }

    @Data
    public static class Jwt {
        private String secret = "change-this-secret";
        private int accessExpSeconds = 3600;
        private int refreshExpDays = 7;
    }
}
