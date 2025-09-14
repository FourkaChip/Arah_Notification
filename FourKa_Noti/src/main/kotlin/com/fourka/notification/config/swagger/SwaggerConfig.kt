package com.fourka.notification.config.swagger

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI {
        // 스키마 이름 — HTTP Bearer 인증 헤더를 사용
        val jwtSchemeName = "Authorization"

        // 모든 API에 대해 해당 스키마 적용
        val securityRequirement = SecurityRequirement().addList(jwtSchemeName)

        // Components 에 스키마 등록
        val components = Components()
            .addSecuritySchemes(jwtSchemeName, SecurityScheme()
                .name(jwtSchemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme("Bearer")
                .bearerFormat("JWT")
            )

        return OpenAPI()
            .components(components)
            .addSecurityItem(securityRequirement)
            .info(
                Info()
                    .title("Notification Service API 명세서")
                    .description("Notification Service API 명세서입니다.")
                    .version("1.0.0")
            )
    }
}