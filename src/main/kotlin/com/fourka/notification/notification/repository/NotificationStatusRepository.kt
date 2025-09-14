package com.fourka.notification.notification.repository

import com.fourka.notification.notification.domain.NotificationStatus
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface NotificationStatusRepository : CoroutineCrudRepository<NotificationStatus, Long> {

    // 로그인한 관리자의 모든 알림 읽음 처리
    @Modifying
    @Query("""
        INSERT INTO notification_status (noti_id, reader_id)
        SELECT n.noti_id, :readerId
        FROM notification n
        WHERE n.company_id = :companyId
        ON CONFLICT (noti_id, reader_id) DO NOTHING -- 이미 행이 있으면 건너뜀
        """)
    suspend fun markAllAsRead(readerId: Long, companyId: Long): Long

    // 단일 알림 읽음 처리
    @Modifying
    @Query("""
        INSERT INTO notification_status (noti_id, reader_id)
        SELECT n.noti_id, :readerId
        FROM notification n
        WHERE n.noti_id = :notiId
          AND n.company_id = :companyId
        ON CONFLICT (noti_id, reader_id) DO NOTHING -- 이미 행이 있으면 건너뜀
        """)
    suspend fun markOneAsRead(notiId: Long, readerId: Long, companyId: Long): Long

    // 마지막으로 읽은 알림 조회
    @Query("""
        SELECT ns.noti_id
        FROM notification_status ns
        JOIN notification n ON n.noti_id = ns.noti_id
        WHERE ns.reader_id = :readerId
          AND n.company_id = :companyId
        ORDER BY ns.created_at DESC, ns.noti_id DESC
        LIMIT 1
        """)
    suspend fun findLastReadId(readerId: Long, companyId: Long): Long?
}
