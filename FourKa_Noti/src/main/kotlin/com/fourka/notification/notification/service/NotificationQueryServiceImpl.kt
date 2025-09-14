package com.fourka.notification.notification.service

import com.fourka.notification.config.client.AdminVerifier
import com.fourka.notification.config.error.exception.CommonException
import com.fourka.notification.config.error.exception.ErrorCode
import com.fourka.notification.config.security.CustomUserDetails
import com.fourka.notification.notification.domain.Notification
import com.fourka.notification.notification.dto.response.NotificationListResponseDto
import com.fourka.notification.notification.dto.response.NotificationResponseDto
import com.fourka.notification.notification.dto.response.NotificationCountResponseDto
import com.fourka.notification.notification.dto.response.NotificationIdResponseDto
import com.fourka.notification.notification.repository.NotificationRepository
import com.fourka.notification.notification.repository.NotificationStatusRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service

@Service
class NotificationQueryServiceImpl(
    private val notificationRepository: NotificationRepository,
    private val notificationBroadcaster: NotificationBroadcaster,
    private val adminVerifier: AdminVerifier,
    private val notificationStatusRepository: NotificationStatusRepository,
) : NotificationQueryService {

    private val log = LoggerFactory.getLogger(this::class.java)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun subscribe(
        user: CustomUserDetails,
        lastEventId: Long?
    ): Flow<ServerSentEvent<NotificationResponseDto>> {
        val log = LoggerFactory.getLogger(this::class.java)
        val myCompanyId: Long = user.getCompanyId()

        println("[subscribe] 추출된 유저: ${user.getUserId()} (${user.username}) ${user.getCompanyId()}")

        // 1. 놓친 알림 조회
        val missedFlow: Flow<NotificationResponseDto> = if (lastEventId != null) {
            log.info("[subscribe] lastEventId = {}", lastEventId, myCompanyId)
            notificationRepository
                .findAllByIdGreaterThanAndCompanyId(lastEventId, myCompanyId)
                .onStart { log.info("[missedFlow] 시작 (lastEventId: {}, companyId: {})", lastEventId, myCompanyId) }
                .onEach { notification -> log.info("[missedFlow] emit: {}", notification) }
                .onCompletion { log.info("[missedFlow] 완료") }
                .map { notification -> NotificationResponseDto.from(notification) }

        } else {
            log.info("[subscribe] lastEventId == null (처음 구독)")
            emptyFlow()
        }

        // 2. 실시간 SSE Flow
        val liveFlow: Flow<NotificationResponseDto> =
            notificationBroadcaster.flow(myCompanyId)
                .filter { dto ->
                    val process = dto.companyId == myCompanyId
                    if (!process) log.debug("[liveFlow] filtered out by companyId. dto.companyId={}, myCompanyId={}", dto.companyId, myCompanyId)
                    process
                }
                .onStart { log.info("[liveFlow] 실시간 알림 구독 시작 (companyId={})", myCompanyId) }
                .onEach  { dto -> log.info("[liveFlow] emit: {}", dto) }


        // 3. 두 Flow를 순차적으로 합침 (놓친 거 먼저 → 실시간)
        return flowOf(missedFlow, liveFlow)
            .flattenConcat()
            .onStart { log.info("[flattenConcat] 통합 Flow 시작") }
            .onEach { notification -> log.info("[flattenConcat] emit: {}", notification) }
            .onCompletion { log.info("[flattenConcat] 완료 (모든 알림 송신 끝)") }
            .map { notification ->
                ServerSentEvent.builder(notification)
                    .id(notification.id.toString())
                    .event("notification")
                    .build()
            }
    }

    // 알림 목록 조회
    override suspend fun getNotificationList(
        isRead: Boolean?, offset: Long, user: CustomUserDetails
    ): NotificationListResponseDto =

        try {
            val limit = 5
            val userId = user.getUserId()
            val companyId = user.getCompanyId()

            adminVerifier.assertAdmin(userId) // 유효한 관지라인지 검증

            val rows = notificationRepository.findPageOffsetAsRow(
                readerId = userId,
                companyId = companyId,
                isRead = isRead,
                limit = limit,
                offset = offset
            )

            val list = rows.map { NotificationResponseDto.from(it) }

            // 회사 알림 개수 조회
            val totalCount = notificationRepository.getCompanyTotalCount(companyId)
            val totalPages = (totalCount + limit - 1) / limit

            NotificationListResponseDto.of(list, limit, offset, totalCount, totalPages)

        } catch (e: CommonException) {
            throw e // 관리자 403 에러 그대로 전파
        } catch (e: Exception) {
            log.error("알림 리스트 조회 실패 userId={}, companyId={} isRead={}, offset={}",
                user.getUserId(), user.getCompanyId(), isRead, offset)
            throw CommonException(ErrorCode.FAIL_FETCH_NOTIFICATION_LIST)
        }

    // 안 읽은 알림 개수 조회
    override suspend fun getUnreadCount(user: CustomUserDetails): NotificationCountResponseDto =

        try {
            val userId = user.getUserId()
            val companyId = user.getCompanyId()

            adminVerifier.assertAdmin(userId) // 유효한 관지라인지 검증

            NotificationCountResponseDto.from(notificationRepository.getUnreadCount(userId, companyId))

        } catch (e: Exception) {
            log.error("안 읽은 알림 개수 조회 실패 userId={}, companyId={}", user.getUserId(), user.getCompanyId())
            throw CommonException(ErrorCode.FAIL_FETCH_UNREAD_NOTIFICATION_COUNT)
        }

    // 알림 조회
    override suspend fun getNotification(notificationId: Long, companyId: Long): Notification =
        notificationRepository.findNotification(notificationId, companyId)
            ?: throw CommonException(ErrorCode.NOTIFICATION_NOT_FOUND)

    // 마지막으로 읽은 알림 조회
    override suspend fun getMyLastRead(user: CustomUserDetails): NotificationIdResponseDto =

        try {
            val userId = user.getUserId()
            val companyId = user.getCompanyId()

            adminVerifier.assertAdmin(userId) // 유효한 관지라인지 검증

            val notificationId = notificationStatusRepository.findLastReadId(userId, companyId)
                ?: throw CommonException(ErrorCode.NO_READ_NOTIFICATIONS)

            NotificationIdResponseDto.from(notificationId)

        } catch (e: CommonException) {
            throw e
        } catch (e: Exception) {
            log.error("마지막으로 읽은 알림 조회 실패. userId={}", user.getUserId())
            throw CommonException(ErrorCode.FAIL_FETCH_LAST_READ_NOTIFICATION)
        }
}
