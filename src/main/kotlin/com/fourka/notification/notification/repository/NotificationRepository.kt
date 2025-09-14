package com.fourka.notification.notification.repository

import com.fourka.notification.notification.domain.Notification
import com.fourka.notification.notification.dto.request.NotificationRowDto
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface NotificationRepository : CoroutineCrudRepository<Notification, Long> {

    // 알림 목록 조회
    @Query("""
        WITH base AS (
          SELECT
            n.noti_id    AS id,
            n.created_at AS created_at,
            n.sender_id  AS sender_id,
            n.receiver_id AS receiver_id,
            n.type       AS type,
            EXISTS (
              SELECT 1
              FROM notification_status ns
              WHERE ns.noti_id = n.noti_id
                AND ns.reader_id = :readerId
            )            AS is_read,
            n.department AS department,
            n.description AS description
          FROM notification n
          WHERE n.company_id = :companyId
        )
        SELECT *
        FROM base
        WHERE (:isRead IS NULL OR is_read = :isRead)
        ORDER BY created_at DESC, id DESC
        LIMIT :limit OFFSET :offset
        """)
    suspend fun findPageOffsetAsRow(
        readerId: Long,
        companyId: Long,
        isRead: Boolean?,
        limit: Int,
        offset: Long
    ): List<NotificationRowDto>

    // 안 읽은 알림 개수 조회
    @Query("""
        SELECT COUNT(*)
        FROM notification n
        WHERE n.company_id = :companyId
          AND (n.receiver_id IS NULL OR n.receiver_id = :readerId)
          AND NOT EXISTS (
            SELECT 1
            FROM notification_status ns
            WHERE ns.noti_id = n.noti_id
              AND ns.reader_id = :readerId
          )
        """)
    suspend fun getUnreadCount(readerId: Long, companyId: Long): Long

    fun findAllByIdGreaterThanAndCompanyId(lastId: Long, companyId: Long): Flow<Notification>

    @Query("""
        SELECT *
        FROM notification
        WHERE noti_id = :id
          AND company_id = :companyId
        """)
    suspend fun findNotification(id: Long, companyId: Long): Notification?

    // 회사 전체 알림 개수 조회
    @Query("""
        SELECT COUNT(*) FROM notification n
        WHERE n.company_id = :companyId
        """)
    suspend fun getCompanyTotalCount(companyId: Long): Long
}
