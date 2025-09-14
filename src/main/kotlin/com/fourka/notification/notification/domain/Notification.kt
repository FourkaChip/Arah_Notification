package com.fourka.notification.notification.domain

import com.fourka.notification.notification.enum.NotificationType
import com.fourka.notification.shared.BaseTimeEntity
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("notification")
class Notification private constructor(

    @Id
    @Column("noti_id")
    val id: Long? = null,

    var type: NotificationType,

    var department: String,

    var description: String,

    var senderId: Long,

    var receiverId: Long?,

    val companyId: Long,

) : BaseTimeEntity() {

    // 정적 팩토리 메서드
    companion object {
        fun createNotification(
            type: NotificationType,
            department: String,
            description: String,
            senderId: Long,
            receiverId: Long?,
            companyId: Long,
        ): Notification {
            return Notification(
                id = null,
                type = type,
                department = department,
                description = description,
                senderId = senderId,
                receiverId = receiverId,
                companyId = companyId,
            )
        }
    }
}
