package com.fourka.notification.notification.service

import com.fourka.notification.config.security.CustomUserDetails
import com.fourka.notification.notification.domain.Notification
import com.fourka.notification.notification.dto.response.NotificationListResponseDto
import com.fourka.notification.notification.dto.response.NotificationResponseDto
import com.fourka.notification.notification.dto.response.NotificationCountResponseDto
import com.fourka.notification.notification.dto.response.NotificationIdResponseDto
import kotlinx.coroutines.flow.Flow
import org.springframework.http.codec.ServerSentEvent

interface NotificationQueryService {

    fun subscribe(user:CustomUserDetails, lastEventId: Long?): Flow<ServerSentEvent<NotificationResponseDto>>

    suspend fun getNotificationList(
        isRead: Boolean?, offset: Long, user: CustomUserDetails
    ): NotificationListResponseDto

    suspend fun getUnreadCount(user: CustomUserDetails): NotificationCountResponseDto

    suspend fun getNotification(notificationId: Long, companyId: Long): Notification

    suspend fun getMyLastRead(user: CustomUserDetails): NotificationIdResponseDto
}
