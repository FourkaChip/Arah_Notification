package com.fourka.notification.notification.dto.response

data class NotificationCountResponseDto(
    val count: Long
) {
    companion object {
        fun from(count: Long): NotificationCountResponseDto =
            NotificationCountResponseDto(count)
    }
}
