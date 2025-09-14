package com.fourka.notification.notification.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fourka.notification.notification.dto.response.NotificationResponseDto
import com.fourka.notification.notification.service.NotificationBroadcaster
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Component
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.retry.Retry
import java.time.Duration

@Component
class RedisSubscriber(
    private val container: ReactiveRedisMessageListenerContainer,
    private val broadcaster: NotificationBroadcaster,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private var subscription: Disposable? = null

    companion object {
        private const val TOPIC_PREFIX = "notify"
        private const val PATTERN = "$TOPIC_PREFIX:*"
    }

    @PostConstruct
    fun subscribe() {
        subscription = container.receive(PatternTopic.of(PATTERN))
            .publishOn(Schedulers.boundedElastic()) // 역직렬화 등 보호
            .flatMap { m ->
                val channel   = m.channel                       // e.g. "notify:123"
                val companyId = channel.substringAfter("$TOPIC_PREFIX:", "").trim()
                if (companyId.isBlank()) {
                    log.warn("Skip message without companyId: channel={}", channel)
                    return@flatMap Mono.empty<Void>()
                }

                val json = m.message
                Mono.fromCallable {
                    objectMapper.readValue(json, NotificationResponseDto::class.java)
                }
                    .doOnNext { notif ->
                        // 안전장치: 혹시라도 dto.companyId가 비어오면 채널에서 보정
                        if (notif.companyId == null) notif.companyId = companyId.toLongOrNull()
                        log.info("PubSub consume channel={}, companyId={}, notifId={}", channel, companyId, notif.id)
                    }
                    .flatMap { notif ->
                        // Broadcaster는 코루틴 → Runnable로 감싸 실행
                        Mono.fromCallable {
                            runBlocking { broadcaster.publish(notif) }
                        }.then()
                    }
            }
            .onErrorContinue { e, bad -> log.error("PubSub consume error payload={}, err={}", bad, e.message, e) }
            .retryWhen(
                Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                    .maxBackoff(Duration.ofMinutes(1))
                    .doBeforeRetry { sig -> log.warn("Resubscribe after error: {}", sig.failure().message) }
            )
            .subscribe()

        log.info("Subscribed Redis PubSub with pattern '{}'", PATTERN)
    }

    @PreDestroy
    fun cleanup() {
        subscription?.dispose()
        log.info("RedisSubscriber disposed.")
    }
}
