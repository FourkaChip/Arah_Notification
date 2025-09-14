package com.fourka.notification.notification.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "알림 목록 조회 응답")
data class NotificationListResponseDto(
    @field:Schema(description = "알림 목록 리스트")
    val notificationResponseList: List<NotificationResponseDto>,
    val nextOffset: Long?,
    val hasNext: Boolean,
    val totalCount: Long,
    val totalPages: Long
) {
    companion object {
        fun of(list: List<NotificationResponseDto>, limit: Int,
               currentOffset: Long, totalCount: Long, totalPages: Long) =
            NotificationListResponseDto(
                notificationResponseList = list,
                nextOffset = currentOffset + list.size,
                hasNext = list.size == limit,
                totalCount = totalCount,
                totalPages = totalPages
            )
    }
}
