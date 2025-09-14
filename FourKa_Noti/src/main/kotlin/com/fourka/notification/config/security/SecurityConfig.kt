package com.fourka.notification.config.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    private val jwtTokenWebFilter: JwtTokenWebFilter
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder =
        PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            // CSRF, 기본 인증, form 로그인 비활성화 (6.1+ Lambda DSL)
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }

            // Stateless 세션 관리
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

            // 허용할 경로, 그 외는 인증 필요
            .authorizeExchange { auth ->
                auth
                    // preflight, OPTIONS 요청 전체 허용
                    .pathMatchers(HttpMethod.OPTIONS).permitAll()
                    .pathMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/docs"
                    ).permitAll() // swagger url
                    .pathMatchers("/api/tests").permitAll() // 테스트용 API
                    //.pathMatchers("/api/notifications", "/api/notifications/unread-count", "api/notifications/read-all").permitAll() // 임시
                    // 토큰 없이 접근 허용할 API
                    .pathMatchers(
                        "/api/users/companies",
                        "/api/users/signup",
                        "/api/users/departments",
                        "/api/users/auth/login",
                        "/api/users/master/login",
                        "/api/users/master/verify-code/send/master",
                        "/api/users/master/verify-code/confirm/master",
                        "/api/users/auth/login2",
                        "/api/notifications/subscribe"
                    ).permitAll()
                    // 그 외 모든 요청은 인증된 사용자만
                    .anyExchange().authenticated()
                    //.anyExchange().permitAll()
            }
            //추후 cors 설정
            /*.cors { cors ->
                cors.configurationSource { exchange ->
                    val config = CorsConfiguration()
                    config.allowedOrigins = listOf(
                        "http://localhost:3000",
                        "http://localhost:5173",
                    )
                    config.allowedMethods = listOf("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                    config.allowedHeaders = listOf("*")
                    config.exposedHeaders = listOf("Content-Type", "Authorization", "Authorization-refresh", "accept")
                    config.allowCredentials = true
                    config
                }
            }*/
            // JWT 필터를 인증 단계에 삽입
            .addFilterAt(jwtTokenWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            // 빌드
            .build()
    }
}
