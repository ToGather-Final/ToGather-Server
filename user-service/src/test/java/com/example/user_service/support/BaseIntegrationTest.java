package com.example.user_service.support;

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@Testcontainers
public abstract class BaseIntegrationTest {

    protected static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    protected static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7.2").withExposedPorts(6379);

    @Autowired protected MockMvc mockMvc;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        startIfNeeded();
        registry.add("spring.datasource.url", () -> mysql.getJdbcUrl());
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
        registry.add("spring.data.redis.host", () -> redis.getHost());
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("jwt.secret", () -> "test-test-test-secret-please-change");
    }

    @BeforeEach
    void beforeEach(){}

    private static void startIfNeeded() {
        if (!mysql.isRunning()) {
            mysql.start();
        }
        if (!redis.isRunning()) {
            redis.start();
        }
    }
}
