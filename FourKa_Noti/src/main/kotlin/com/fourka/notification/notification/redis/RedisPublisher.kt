package com.fourka.notification.notification.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fourka.notification.config.error.exception.CommonException
import com.fourka.notification.config.error.exception.ErrorCode
import com.fourka.notification.notification.dto.response.NotificationResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import reactor.util.retry.Retry
import java.time.Duration

@Component
class RedisPublisher(
    @Qualifier("pubSubRedisTemplate")
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun publish(notification: NotificationResponseDto) {
        val companyId = requireNotNull(notification.companyId) { "companyId required" }
        val channel = "notify:$companyId"
        val payload = objectMapper.writeValueAsString(notification)

        redisTemplate.convertAndSend(channel, payload)
            .doOnError { e -> log.error("pubsub failed channel=$channel: ${e.message}", e) }
            .subscribe()
    }
}
