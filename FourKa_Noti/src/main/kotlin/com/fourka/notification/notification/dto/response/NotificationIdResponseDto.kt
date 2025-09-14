package com.fourka.notification.notification.dto.response

data class NotificationIdResponseDto (
    val id: Long
) {
    companion object {
        fun from(id: Long): NotificationIdResponseDto =
            NotificationIdResponseDto(id)
    }
}