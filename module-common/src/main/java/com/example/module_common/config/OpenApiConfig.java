package com.example.module_common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:Unknown Service}")
    private String serviceName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
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
                ));
    }
}
