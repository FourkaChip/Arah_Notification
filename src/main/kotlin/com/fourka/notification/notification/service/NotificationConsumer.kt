package com.fourka.notification.notification.service

import com.fourka.notification.config.error.exception.CommonException
import com.fourka.notification.config.error.exception.ErrorCode
import com.fourka.notification.notification.dto.request.NotificationStreamDto
import com.fourka.notification.notification.dto.response.NotificationResponseDto
import com.fourka.notification.notification.redis.RedisPublisher
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.stream.*
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.time.Duration
import java.util.*
import java.util.stream.Collectors

@Component
class NotificationConsumer(
    private val redisTemplate: ReactiveRedisTemplate<String, MapRecord<String, String, String>>,
    private val notificationService: NotificationCommandService,
    private val publisher: RedisPublisher
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val streamKey = "notification-stream"
    private val groupName = "notification-group"
    private val consumerName = UUID.randomUUID().toString()

    @PostConstruct
    fun init() {
        ensureConsumerGroupExists()
        scope.launch { consumeLoop() }
        scope.launch { checkAndClaimPendingMessages() }
    }

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }

    private fun ensureConsumerGroupExists() {

        redisTemplate
            .opsForStream<String, String>()
            .createGroup(streamKey, ReadOffset.latest(), groupName)
            .onErrorResume { e ->
                val msg = e.message.orEmpty()
                if (msg.contains("BUSYGROUP")) Mono.empty()
                else Mono.error(e)
            }.subscribe()
    }

    private suspend fun consumeLoop() {
        val ops = redisTemplate.opsForStream<String, MapRecord<String, String, String>>()

        while (scope.isActive) {
            try {
                val records = ops.read(
                    Consumer.from(groupName, consumerName),
                    StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
                    StreamOffset.create(streamKey, ReadOffset.lastConsumed())
                )
                    .collectList()
                    .awaitSingleOrNull()

                records?.forEach { record ->
                    val map = record.value as? Map<String, String> ?: return@forEach

                    val streamDto = NotificationStreamDto(
                        senderId = map["senderId"]?.toLong() ?: return@forEach,
                        receiverId = map["receiverId"]?.toLong(), // nullable
                        type = map["type"] ?: return@forEach,
                        department = map["department"] ?: "",
                        description = map["description"] ?: "",
                        companyId = map["companyId"]?.toLong() ?: return@forEach,
                    )

                    val notification = NotificationResponseDto.from(
                        notificationService.createNotification(streamDto)
                    )

                    publisher.publish(notification)

                    ops.acknowledge(groupName, record).awaitFirstOrNull()
                }
            } catch (e: Exception) {
                log.error("Error: ${e.message}")
                delay(1000) // 재시도
            }
        }
    }

    private suspend fun checkAndClaimPendingMessages() {
        while (scope.isActive) {
            try {
                // 1. 저수준 명령이 필요할 때마다 '지역적으로' 연결을 얻습니다. (상태 공유 방지)
                val streamCommands = redisTemplate.connectionFactory.reactiveConnection.streamCommands()
                val key = ByteBuffer.wrap(streamKey.toByteArray())

                // 2. XPENDING 명령 실행 (전체 옵션 사용)
                val summary = streamCommands.xPending(
                    key,
                    groupName,
                    Range.unbounded<String>(),
                    30L // 한 번에 확인할 최대 메시지 수
                ).awaitSingleOrNull()

                if (summary == null) {
                    delay(10_000)
                    continue
                }

                // 3. .get()으로 Java Stream을 얻고, List로 변환하여 올바르게 처리합니다.
                val pendingMessageList = summary.get().collect(Collectors.toList())

                val targetIds = pendingMessageList
                    .filter { p -> p.elapsedTimeSinceLastDelivery > Duration.ofMinutes(1) }
                    .map { it.id }

                if (targetIds.isNotEmpty()) {
                    // 4. XCLAIM으로 메시지 소유권 가져오기
                    val claimedRecords = streamCommands.xClaim(
                        key,
                        groupName,
                        consumerName,
                        Duration.ofMinutes(1), // 재처리 시간
                        *targetIds.toTypedArray()
                    ).collectList().awaitSingle()

                    for (rec in claimedRecords) {
                        try {
                            // ACK는 고수준 Template을 사용하는 안전한 헬퍼 함수로 처리
                            handleClaimedRecord(rec)
                        } catch (ex: Exception) {
                            log.error("Claimed record processing failed id={} : {}", rec.id, ex.message, ex)
                        }
                    }
                } else {
                    delay(10_000)
                }
            } catch (e: Exception) {
                log.error("Error in pending/claim logic: ${e.message}")
                delay(10_000)
            }
        }
    }

    private suspend fun handleClaimedRecord(rec: MapRecord<ByteBuffer, ByteBuffer, ByteBuffer>) {
        // 1) ByteBuffer -> String 맵 디코딩
        val payload = decodeRecord(rec)

        // 2) 유효성/DTO 변환
        val streamDto = NotificationStreamDto(
            senderId = payload["senderId"]?.toLong() ?: throw CommonException(ErrorCode.SENDER_ID_NULL),
            receiverId = payload["receiverId"]?.toLong(),
            type = payload["type"] ?: throw CommonException(ErrorCode.NOTIFICATION_TYPE_NOY_EXIST),
            department = payload["department"] ?: "",
            description = payload["description"] ?: "",
            companyId = payload["companyId"]?.toLong() ?: throw CommonException(ErrorCode.COMPANY_ID_NULL),
        )

        // 3) 비즈니스 처리 (DB 저장 + 전송)
        val notification = NotificationResponseDto.from(
            notificationService.createNotification(streamDto)
        )
        publisher.publish(notification)

        // 4. ACK는 레코드 ID 문자열(`.value`)만 사용하여 직접 호출하는 것이 가장 간단하고 올바른 방법입니다.
        redisTemplate.opsForStream<String, String>()
            .acknowledge(streamKey, groupName, rec.id.value)
            .awaitFirstOrNull()
    }

    private fun decode(bb: ByteBuffer): String =
        java.nio.charset.StandardCharsets.UTF_8.decode(bb.asReadOnlyBuffer()).toString()

    private fun decodeRecord(rec: MapRecord<ByteBuffer, ByteBuffer, ByteBuffer>): Map<String, String> =
        rec.value.entries.associate {
            decode(it.key) to decode(it.value)
        }
}
