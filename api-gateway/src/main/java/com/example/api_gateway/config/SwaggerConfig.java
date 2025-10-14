package com.example.api_gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ToGather API Gateway")
                        .description("ToGather 전체 서비스 API 통합 문서")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server().url("http://localhost:8000").description("API Gateway"),
                        new Server().url("http://localhost:8002").description("User Service"),
                        new Server().url("http://localhost:8001").description("Trading Service"),
                        new Server().url("http://localhost:8003").description("Pay Service"),
                        new Server().url("http://localhost:8004").description("Vote Service")
                ));
    }
}
