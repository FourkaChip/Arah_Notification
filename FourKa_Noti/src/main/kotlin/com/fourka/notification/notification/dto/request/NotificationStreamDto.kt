package com.fourka.notification.notification.dto.request

//추후 스트림용과 RestAPI 용을 분리할 경우 유효성 검증 필요
data class NotificationStreamDto (
    val senderId: Long,
    val receiverId: Long?,
    val type: String,
    val department: String,
    val description: String,
    val companyId: Long,
)
