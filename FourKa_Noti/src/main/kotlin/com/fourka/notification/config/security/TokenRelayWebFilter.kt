package com.fourka.notification.config.security

import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

// 들어온 요청의 Authorization 헤더에서 Bearer 토큰만 뽑아서 Reactor Context에 저장하는 필터
@Component
class TokenRelayWebFilter : WebFilter, Ordered {

    companion object {
        const val AUTH_TOKEN_CTX_KEY = "AUTH_TOKEN"
    }

    override fun getOrder(): Int =
        SecurityWebFiltersOrder.AUTHENTICATION.order - 1 // 시큐리티 인증 필터 직전에 토큰을 Reactor Context에 넣어 안정적으로 토큰을 릴레이
                                                         // -> WebClient가 Reactor Context의 토큰을 무조건 읽게 됨

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val bearer = extractBearer(exchange)

        return if (!bearer.isNullOrBlank()) {
            chain.filter(exchange).contextWrite { ctx ->
                // 이미 컨텍스트에 있으면 보존, 없으면 주입
                if (ctx.hasKey(AUTH_TOKEN_CTX_KEY)) ctx
                else ctx.put(AUTH_TOKEN_CTX_KEY, bearer)
            }
        } else {
            chain.filter(exchange)
        }
    }

    private fun extractBearer(exchange: ServerWebExchange): String? {
        // Authorization 헤더 우선
        val raw = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)?.trim()
        val fromHeader = raw
            ?.takeIf { it.startsWith("Bearer", ignoreCase = true) }
            ?.substringAfter(' ', missingDelimiterValue = "")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        if (!fromHeader.isNullOrBlank()) return fromHeader

        // (옵션) SSE 등 헤더 못 쓰는 케이스 대비 - 쿼리스트링에서 토큰 허용
        val qp = exchange.request.queryParams
        val fromQuery = (qp.getFirst("access_token") ?: qp.getFirst("token"))
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        return fromQuery
    }
}
