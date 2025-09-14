package com.fourka.notification.config.security

import com.fourka.notification.config.redis.RedisService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class JwtTokenWebFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val redisService: RedisService
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange) // 토큰이 없으면 바로 다음으로 넘김
        }

        val token = authHeader.substring(7)

        // Mono.defer를 사용하여 각 구독(요청)에 대해 새로운 처리 흐름을 생성합니다.
        return Mono.defer {
            // 1. 토큰 유효성 검증 (동기)
            if (!jwtTokenProvider.validateToken(token)) {
                return@defer Mono.error(RuntimeException("유효하지 않은 토큰"))
            }

            // 2. 블랙리스트 확인 (비동기 suspend 함수)
            // mono 빌더로 suspend 함수를 Mono로 감싸고 flatMap으로 연결합니다.
            mono { redisService.isBlacklisted(token) }
                .flatMap { isBlacklisted ->
                    if (isBlacklisted) {
                        Mono.error(RuntimeException("블랙리스트 토큰"))
                    } else {
                        Mono.just(true) // 성공 시 다음 단계로 진행
                    }
                }
        }.flatMap {
            // 3. 모든 검증 통과 시, SecurityContext를 생성하고 체인을 계속 진행
            val claims = jwtTokenProvider.extractClaims(token)
            // ... (기존의 claims 파싱 로직은 여기에 위치)
            val rawUserId = claims["user_id"]
            val userId = when (rawUserId) {
                is Int -> rawUserId.toLong()
                is Long -> rawUserId
                else -> throw RuntimeException("Invalid user_id type")
            }
            val rawCompanyId = claims["company_id"]
            val companyId = when (rawCompanyId) {
                is Int -> rawCompanyId.toLong()
                is Long -> rawCompanyId
                else -> throw RuntimeException("Invalid company_id type")
            }
            val email = claims.subject
            val role = claims["role", String::class.java]

            val principal = CustomUserDetails(userId, email, role, companyId)
            val auth = UsernamePasswordAuthenticationToken(principal, token, principal.authorities)

            // contextWrite로 컨텍스트를 추가하고 'await' 없이 바로 다음 체인으로 넘깁니다.
            chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
        }.onErrorResume { error ->
            // 4. 스트림 처리 중 에러 발생 시 공통으로 처리
            println("인증 실패: ${error.message}")
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            exchange.response.headers.contentType = MediaType.APPLICATION_JSON
            val buffer = exchange.response.bufferFactory().wrap(
                """{"error":"invalid token"}""".toByteArray()
            )
            exchange.response.writeWith(Mono.just(buffer))
        }
    }
}
