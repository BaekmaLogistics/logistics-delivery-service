package com.sparta.logistics.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        // SecurityScheme 이름 정의
        String jwtSchemeName = "jwtAuth";

        // API 요청 시 SecurityRequirement 적용
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

        // Bearer Token 방식의 Components 구성
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));
        return new OpenAPI()
                .servers(List.of(new Server().url("/"))) // Gateway 경로 유지를 위한 상대 경로 설정
                .addSecurityItem(securityRequirement)
                .components(components)
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Delivery Service API")
                        .version("v1.0"));
    }
}
