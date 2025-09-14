package com.fourka.notification.notification.domain

import com.fourka.notification.shared.BaseTimeEntity
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("notification_status")
class NotificationStatus private constructor(

    @Id
    @Column("noti_status_id")
    val id: Long? = null,

    @Column("noti_id")
    val notiId: Long,

    @Column("reader_id")
    val readerId: Long,

    @Column("is_read")
    var isRead: Boolean = false

) : BaseTimeEntity() {

    // 정적 팩토리 메서드
    companion object {
        fun createNotificationStatus(notiId: Long, readerId: Long): NotificationStatus =
            NotificationStatus(
                id = null,
                notiId = notiId,
                readerId = readerId,
                isRead = false
            )
    }
}
