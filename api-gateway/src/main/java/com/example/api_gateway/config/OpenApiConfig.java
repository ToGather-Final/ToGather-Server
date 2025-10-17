package com.example.api_gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    @Primary
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(createInfo())
                .servers(createServers())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(createComponents())
                .tags(createTags());
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("인증 관리")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("사용자 관리")
                .pathsToMatch("/api/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi groupApi() {
        return GroupedOpenApi.builder()
                .group("그룹 관리")
                .pathsToMatch("/api/groups/**")
                .build();
    }

    @Bean
    public GroupedOpenApi tradingApi() {
        return GroupedOpenApi.builder()
                .group("트레이딩 관리")
                .pathsToMatch("/api/trading/**")
                .build();
    }

    @Bean
    public GroupedOpenApi payApi() {
        return GroupedOpenApi.builder()
                .group("결제 관리")
                .pathsToMatch("/api/pay/**")
                .build();
    }

    @Bean
    public GroupedOpenApi voteApi() {
        return GroupedOpenApi.builder()
                .group("투표 관리")
                .pathsToMatch("/api/vote/**")
                .build();
    }

    @Bean
    public GroupedOpenApi historyApi() {
        return GroupedOpenApi.builder()
                .group("히스토리 관리")
                .pathsToMatch("/api/history/**")
                .build();
    }

    private Info createInfo() {
        return new Info()
                .title("ToGather API Gateway")
                .description("ToGather 전체 서비스 API 통합 문서")
                .version("1.0.0")
                .contact(new Contact()
                        .name("ToGather API Support")
                        .email("support@togather.com"))
                .license(new License()
                        .name("MIT")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private List<Server> createServers() {
        return List.of(
                new Server()
                        .url("http://localhost:8000")
                        .description("API Gateway (로컬 개발 환경)"),
                new Server()
                        .url("http://localhost:8081")
                        .description("Trading Service (로컬 개발 환경)"),
                new Server()
                        .url("http://localhost:8082")
                        .description("User Service (로컬 개발 환경)"),
                new Server()
                        .url("http://localhost:8083")
                        .description("Pay Service (로컬 개발 환경)"),
                new Server()
                        .url("http://localhost:8084")
                        .description("Vote Service (로컬 개발 환경)")
        );
    }

    private Components createComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth", createBearerAuthScheme());
    }

    private SecurityScheme createBearerAuthScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                    JWT 토큰을 입력하세요.
                    
                    **토큰 획득 방법:**
                    1. 회원가입 또는 로그인 API 호출
                    2. 응답에서 `accessToken` 값 복사
                    3. Authorization 헤더에 `Bearer {accessToken}` 형태로 입력
                    
                    **예시:**
                    ```
                    Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                    ```
                    """);
    }

    private List<Tag> createTags() {
        return List.of(
                new Tag()
                        .name("인증 관리")
                        .description("회원가입, 로그인, 토큰 갱신, 로그아웃 관련 API"),
                new Tag()
                        .name("사용자 관리")
                        .description("사용자 정보 조회, 수정, 중복 확인 관련 API"),
                new Tag()
                        .name("그룹 관리")
                        .description("그룹 생성, 조회, 수정, 삭제 관련 API"),
                new Tag()
                        .name("트레이딩 관리")
                        .description("주식 거래, 포트폴리오 관리, 계좌 관리 관련 API"),
                new Tag()
                        .name("결제 관리")
                        .description("결제, 이체, 계좌 관리 관련 API"),
                new Tag()
                        .name("투표 관리")
                        .description("투표 제안, 참여, 집계 관련 API"),
                new Tag()
                        .name("히스토리 관리")
                        .description("사용자 활동 히스토리 조회 관련 API")
        );
    }
}