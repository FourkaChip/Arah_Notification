package com.fourka.notification.config.client

import com.fourka.notification.config.security.TokenRelayWebFilter.Companion.AUTH_TOKEN_CTX_KEY
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

// User 서비스 호출용 WebClient 빈을 만들고, Reactor Context에 들어있는 액세스 토큰을 자동으로 Authorization 헤더에 붙여줌
@Configuration
class UserServiceClientConfig(
    @Value("\${user-service.base-url}") private val baseUrl: String
) {
    @Bean
    fun userWebClient(builder: WebClient.Builder): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2_000) // TCP 연결 시도 최대 2초
            .responseTimeout(Duration.ofSeconds(3))     // 응담 대기 최대 3초
            .doOnConnected {
                it.addHandlerLast(ReadTimeoutHandler(3, TimeUnit.SECONDS)) // 소켓 읽기/쓰기 타임아웃 3초
                    .addHandlerLast(WriteTimeoutHandler(3, TimeUnit.SECONDS))
            }

        /**
         * TokenRelayWebFilter가 요청 들어올 때 Reactor Context에 토큰을 넣어둠
         * 이 필터가 토큰을 꺼내서 모든 WebClient 호출에 Authorization 헤더 자동 부착
         * 토큰이 없으면 원본 요청 그대로 진행
         */
        val tokenRelayFilter = ExchangeFilterFunction { request, next ->
            Mono.deferContextual { ctx ->
                val token: String? =
                    if (ctx.hasKey(AUTH_TOKEN_CTX_KEY)) ctx.get(AUTH_TOKEN_CTX_KEY) as String else null

                val mutated = if (!token.isNullOrBlank()) {
                    ClientRequest.from(request)
                        .headers { h -> h.setBearerAuth(token) }
                        .build()
                } else {
                    request
                }
                next.exchange(mutated)
            }
        }

        return builder
            .baseUrl(baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .filter(tokenRelayFilter)
            .build()
    }
}
