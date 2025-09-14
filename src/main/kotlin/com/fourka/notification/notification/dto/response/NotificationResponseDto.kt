package com.fourka.notification.notification.dto.response

import com.fourka.notification.config.error.exception.CommonException
import com.fourka.notification.config.error.exception.ErrorCode
import com.fourka.notification.notification.domain.Notification
import com.fourka.notification.notification.dto.request.NotificationRowDto
import com.fourka.notification.notification.enum.NotificationType
import java.time.LocalDateTime

data class NotificationResponseDto(
    val id: Long,
    val createdAt: LocalDateTime?,
    val senderId: Long,
    val receiverId: Long?,
    val type: NotificationType,
    val isRead: Boolean,
    val content: Content,
    var companyId: Long?,
) {
    data class Content(
        val department: String,
        val description: String
    )

    companion object {
        fun from(notification: Notification): NotificationResponseDto {
            return NotificationResponseDto(
                id = notification.id ?: throw CommonException(ErrorCode.NOTIFICATION_ID_NULL),
                createdAt = notification.createdAt ?: LocalDateTime.now(),
                senderId = notification.senderId,
                receiverId = notification.receiverId,
                type = notification.type,
                isRead = false, // 임시로 false 부여
                content = Content(
                    department = notification.department,
                    description = notification.description
                ),
                companyId = notification.companyId,
            )
        }

        fun from(row: NotificationRowDto) = NotificationResponseDto(
            id = row.id,
            createdAt = row.createdAt,
            senderId = row.senderId,
            receiverId = row.receiverId,
            type = row.type,
            isRead = row.isRead,
            content = Content(row.department, row.description),
            companyId = null, // 필요없는 데이터라 null 처리
        )
    }
}
