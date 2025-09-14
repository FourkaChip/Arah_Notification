package com.fourka.notification.notification.dto.request

import com.fourka.notification.notification.enum.NotificationType
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDateTime

data class NotificationRowDto (
    @Column("id")
    val id: Long,

    @Column("created_at")
    val createdAt: LocalDateTime?,

    @Column("sender_id")
    val senderId: Long,

    @Column("receiver_id")
    val receiverId: Long?,

    @Column("type")
    val type: NotificationType,

    @Column("is_read")
    val isRead: Boolean,

    @Column("department")
    val department: String,

    @Column("description")
    val description: String
)
