package com.fourka.notification.config.client

import com.fourka.notification.config.dto.AdminBrief
import com.fourka.notification.config.dto.ApiResponse
import com.fourka.notification.config.error.exception.CommonException
import com.fourka.notification.config.error.exception.ErrorCode
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

// User 서비스의 관리자 목록을 가져와서 userId Set으로 반환
@Component
class UserClientImpl(
    private val webClient: WebClient
) : UserClient {

    private val log = LoggerFactory.getLogger(this::class.java)

    // 정상적으로 파싱하기 위해 역직렬화
    private val adminListType =
        object : ParameterizedTypeReference<ApiResponse<List<AdminBrief>>>() {}

    override suspend fun fetchAdminIds(): Set<Long> {
        val resp = webClient.get()
            .uri("/api/users/admins")
            .exchangeToMono { res -> // HTTP 상태 코드 먼저 확인
                if (res.statusCode().value() == 401 || res.statusCode().value() == 403) {
                    res.bodyToMono(String::class.java).flatMap { body ->
                        log.warn("관리자 조회 API - /api/users/admins -> {} body={}", res.statusCode(), body)
                        Mono.error(CommonException(ErrorCode.UNAUTHORIZED_ADMIN))
                    }
                } else {
                    res.bodyToMono(adminListType)
                }
            }
            .awaitSingle() // 코루틴, 리액터 브릿지 -> 논블로킹 유지

        // userId만 뽑아 Set<Long>으로 반환
        val list = if (resp.success) (resp.result ?: emptyList()) else emptyList()

        return list.map { it.userId }.toSet()
    }
}
