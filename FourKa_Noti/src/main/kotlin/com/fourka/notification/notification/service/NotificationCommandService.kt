package com.fourka.notification.notification.service

import com.fourka.notification.config.security.CustomUserDetails
import com.fourka.notification.notification.domain.Notification
import com.fourka.notification.notification.dto.request.NotificationStreamDto
import com.fourka.notification.notification.dto.response.NotificationCountResponseDto
import com.fourka.notification.notification.dto.response.NotificationIdResponseDto

interface NotificationCommandService {
    suspend fun createNotification(streamDto: NotificationStreamDto): Notification

    suspend fun sendNotification(requestDto: NotificationStreamDto, user: CustomUserDetails): Boolean

    suspend fun markAllAsRead(user: CustomUserDetails): NotificationCountResponseDto

    suspend fun markAsRead(user: CustomUserDetails, notificationId: Long): NotificationIdResponseDto
}
