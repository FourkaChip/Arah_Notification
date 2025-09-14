package com.fourka.notification.shared

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import java.time.ZoneId

abstract class BaseTimeEntity {

    @CreatedDate
    var createdAt: LocalDateTime? = null

    @LastModifiedDate
    var updatedAt: LocalDateTime? = null

    val createdAtSeoul: LocalDateTime?
        get() = createdAt
            ?.atZone(ZoneId.of("UTC"))
            ?.withZoneSameInstant(ZoneId.of("Asia/Seoul"))
            ?.toLocalDateTime()

    val updatedAtSeoul: LocalDateTime?
        get() = updatedAt
            ?.atZone(ZoneId.of("UTC"))
            ?.withZoneSameInstant(ZoneId.of("Asia/Seoul"))
            ?.toLocalDateTime()
}