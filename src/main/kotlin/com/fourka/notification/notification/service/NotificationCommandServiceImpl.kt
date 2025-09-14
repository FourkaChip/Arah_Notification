package com.fourka.notification.notification.service

import com.fourka.notification.config.client.AdminVerifier
import com.fourka.notification.config.error.exception.CommonException
import com.fourka.notification.config.error.exception.ErrorCode
import com.fourka.notification.config.security.CustomUserDetails
import com.fourka.notification.notification.domain.Notification
import com.fourka.notification.notification.dto.request.NotificationStreamDto
import com.fourka.notification.notification.dto.response.NotificationCountResponseDto
import com.fourka.notification.notification.dto.response.NotificationIdResponseDto
import com.fourka.notification.notification.enum.NotificationType
import com.fourka.notification.notification.repository.NotificationRepository
import com.fourka.notification.notification.repository.NotificationStatusRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait

@Service
class NotificationCommandServiceImpl(
    private val notificationRepository: NotificationRepository,
    private val redisTemplate: ReactiveRedisTemplate<String, MapRecord<String, String, String>>,
    private val operator: TransactionalOperator,
    private val notificationQueryService: NotificationQueryService,
    private val adminVerifier: AdminVerifier,
    private val notificationStatusRepository: NotificationStatusRepository,
) : NotificationCommandService {

    private val log = LoggerFactory.getLogger(this::class.java)

    override suspend fun createNotification(streamDto: NotificationStreamDto): Notification {
        val notification = Notification.createNotification(
            senderId = streamDto.senderId,
            receiverId = streamDto.receiverId,
            type = NotificationType.valueOf(streamDto.type),
            department = streamDto.department,
            description = streamDto.description,
            companyId = streamDto.companyId,
        )

        return notificationRepository.save(notification)
    }

    override suspend fun sendNotification(requestDto: NotificationStreamDto, user: CustomUserDetails): Boolean {
        val map = mapOf(
            "senderId" to requestDto.senderId.toString(),
            "receiverId" to requestDto.receiverId.toString(),
            "type" to requestDto.type,
            "department" to requestDto.department,
            "description" to requestDto.description
        )
        return try {
            redisTemplate
                .opsForStream<String, MapRecord<String, String, String>>()
                .add(MapRecord.create("notification-stream", map))
                .awaitSingleOrNull() != null
        } catch (e: Exception) {
            throw CommonException(ErrorCode.FAIL_SEND_NOTIFICATION)
        }
    }

    // 모든 알림 읽음 처리
    override suspend fun markAllAsRead(user: CustomUserDetails): NotificationCountResponseDto =
        try {
            val userId = user.getUserId()
            val companyId = user.getCompanyId()

            adminVerifier.assertAdmin(userId) // 유효한 관지라인지 검증

            // 트랜잭션 범위 안에서 업데이트 메서드 호출
            val updatedCount: Long = operator.executeAndAwait {
                notificationStatusRepository.markAllAsRead(userId, companyId)
            }

            if (updatedCount == 0L) {
                throw CommonException(ErrorCode.NO_UNREAD_NOTIFICATIONS)
            }

            log.info("읽음 처리한 알림 개수: {}", updatedCount)
            NotificationCountResponseDto.from(updatedCount)

        } catch (e: CommonException) {
            throw e
        } catch (e: Exception) {
            log.error("모든 알림 읽음 처리 실패. userId={}", user.getUserId())
            throw CommonException(ErrorCode.FAIL_READ_NOTIFICATION)
        }

    // 단일 알림 읽음 처리
    override suspend fun markAsRead(user: CustomUserDetails, notificationId: Long): NotificationIdResponseDto =
        try {
            val userId = user.getUserId()
            val companyId = user.getCompanyId()

            adminVerifier.assertAdmin(userId) // 유효한 관지라인지 검증

            // 트랜잭션 범위 안에서 업데이트 메서드 호출
            operator.executeAndAwait {
                // 알림 조회
                notificationQueryService.getNotification(notificationId, companyId)

                // 알림 상태 업데이트
                val updatedCount = notificationStatusRepository.markOneAsRead(notificationId, userId, companyId)
                if (updatedCount == 0L) {
                    throw CommonException(ErrorCode.ALREADY_READ_NOTIFICATION)
                }

                NotificationIdResponseDto.from(notificationId)
            }.also {
                log.info("읽음 처리한 알림 번호: {}", notificationId)
            }
        } catch (e: CommonException) {
            throw e
        } catch (e: Exception) {
            log.error("단일 알림 읽음 처리 실패. userId={}", user.getUserId())
            throw CommonException(ErrorCode.FAIL_READ_NOTIFICATION)
        }
}
