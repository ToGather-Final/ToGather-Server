package com.example.module_common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:Unknown Service}")
    private String serviceName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    @Profile("!dev")
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ToGather " + serviceName + " API")
                        .description("ToGather " + serviceName + " API 문서")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ToGather Team")
                                .email("support@togather.com")
                                .url("https://togather.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("개발 서버"),
                        new Server()
                                .url("https://api.togather.com")
                                .description("운영 서버")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 입력하세요. 예: your-jwt-token")));
    }

    @Bean
    @Profile("dev")
    public OpenAPI customOpenAPIDev() {
        return new OpenAPI()
                .info(new Info()
                        .title("ToGather " + serviceName + " API")
                        .description("ToGather " + serviceName + " API 문서")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ToGather Team")
                                .email("support@togather.com")
                                .url("https://togather.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("개발 서버"),
                        new Server()
                                .url("https://api.togather.com")
                                .description("운영 서버")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .addSecurityItem(new SecurityRequirement().addList("xUserContext"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 입력하세요. 예: your-jwt-token"))
                        .addSecuritySchemes("xUserContext", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-User-Id")
                                .description("DEV ONLY - 사용자 ID를 직접 입력하세요")));
    }
}
