package com.fourka.notification.notification.controller

import com.fourka.notification.config.dto.ApiResponse
import com.fourka.notification.config.security.CustomUserDetails
import com.fourka.notification.config.security.JwtTokenProvider
import com.fourka.notification.notification.dto.request.NotificationStreamDto
import com.fourka.notification.notification.dto.response.NotificationListResponseDto
import com.fourka.notification.notification.dto.response.NotificationResponseDto
import com.fourka.notification.notification.dto.response.NotificationCountResponseDto
import com.fourka.notification.notification.dto.response.NotificationIdResponseDto
import com.fourka.notification.notification.service.NotificationCommandService
import com.fourka.notification.notification.service.NotificationQueryService
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.constraints.Min
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationQueryService: NotificationQueryService,
    private val notificationCommandService: NotificationCommandService
) : NotificationControllerInterface {

    @Operation(summary = "SSE 구독 API", description = "SSE 구독 API입니다.")
    @GetMapping("/subscribe", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    override fun subscribe(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestHeader(value = "Last-Event-Id", required = false) lastEventId: Long?
    ): Flow<ServerSentEvent<NotificationResponseDto>> {

        println("[subscribe] 구독 요청: user=$user, companyId=${user.getCompanyId()}, lastEventId=$lastEventId")

        val flow = notificationQueryService.subscribe(user, lastEventId)
            .onStart { println("[subscribe] 알림 SSE 스트림 시작") }
            .onCompletion { println("[subscribe] SSE 스트림 종료") }

        return flow
    }

    //여분 Rest API 요청
    @Operation(summary = "알림 전송 예비 API", description = "알림을 전송하는 예비 API입니다.")
    @PostMapping("/send")
    suspend fun send(
        @RequestBody request: NotificationStreamDto,
        @AuthenticationPrincipal user: CustomUserDetails
    ): ApiResponse<Boolean> = ApiResponse.create(notificationCommandService.sendNotification(request, user))

    /**
     * 알림 목록 조회
     * 전체 알림 조회, 읽은/안읽은 필터링은 isRead 쿼리 파라미터로 처리
     * GET /api/notifications              → 전체 알림 조회
     * GET /api/notifications?isRead=true  → 읽은 알림만 조회
     * GET /api/notifications?isRead=false → 안읽은 알림만 조회
     */
    @Operation(summary = "알림 목록 조회 API", description = "알림 목록을 조회하는 API입니다.")
    @GetMapping
    override suspend fun getNotificationList(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestParam(name = "isRead", required = false) isRead: Boolean?,
        @RequestParam(name = "offset", defaultValue = "0") @Min(0) offset: Long,
    ): ApiResponse<NotificationListResponseDto> =
        ApiResponse.success(notificationQueryService.getNotificationList(isRead, offset, user))

    // 안 읽은 알림 개수 조회
    @Operation(summary = "안 읽은 알림 개수 조회 API", description = "안 읽은 알림 개수를 조회하는 API입니다.")
    @GetMapping("/unread-count")
    override suspend fun getUnreadNotificationCount(
        @AuthenticationPrincipal user: CustomUserDetails,
    ): ApiResponse<NotificationCountResponseDto> =
        ApiResponse.success(notificationQueryService.getUnreadCount(user))

    // 모든 알림 읽음 처리
    @Operation(summary = "모든 알림 읽음 처리 API", description = "모든 알림을 읽음으로 처리하는 API입니다.")
    @PatchMapping("/read-all")
    override suspend fun markAllAsRead(
        @AuthenticationPrincipal user: CustomUserDetails,
    ): ApiResponse<NotificationCountResponseDto> =
        ApiResponse.success(notificationCommandService.markAllAsRead(user))

    // 단일 알림 읽음 처리
    @Operation(summary = "단일 알림 읽음 처리 API", description = "단일 알림을 읽음으로 처리하는 API입니다.")
    @PatchMapping("/{notificationId}/read")
    override suspend fun markAsRead(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable notificationId: Long
    ): ApiResponse<NotificationIdResponseDto> =
        ApiResponse.success(notificationCommandService.markAsRead(user, notificationId))

    // 마지막으로 읽은 알림 조회
    @Operation(summary = "마지막으로 읽은 알림 조회 API", description = "마지막으로 읽은 알림을 조회하는 API입니다.")
    @GetMapping("/me/last-read")
    override suspend fun getMyLastRead(
        @AuthenticationPrincipal user: CustomUserDetails,
    ): ApiResponse<NotificationIdResponseDto> =
        ApiResponse.success(notificationQueryService.getMyLastRead(user))
}
