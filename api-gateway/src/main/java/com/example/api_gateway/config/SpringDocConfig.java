package com.example.api_gateway.config;

import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
public class SpringDocConfig {

    @Bean
    @Primary
    public SpringDocConfigProperties springDocConfigProperties() {
        SpringDocConfigProperties properties = new SpringDocConfigProperties();
        properties.setPathsToMatch(List.of("/api/**"));
        properties.setPackagesToScan(List.of("com.example.api_gateway.controller"));
        return properties;
    }
}
