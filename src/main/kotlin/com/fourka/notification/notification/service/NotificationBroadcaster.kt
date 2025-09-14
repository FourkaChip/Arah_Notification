package com.fourka.notification.notification.service

import com.fourka.notification.notification.dto.response.NotificationResponseDto
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class NotificationBroadcaster {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val companyFlows =
        ConcurrentHashMap<Long, MutableSharedFlow<NotificationResponseDto>>()

    private val userFlows =
        ConcurrentHashMap<Pair<Long, Long>, MutableSharedFlow<NotificationResponseDto>>()

    private fun companyFlow(companyId: Long) =
        companyFlows.computeIfAbsent(companyId) {
            MutableSharedFlow(
                replay = 0,
                extraBufferCapacity = 1024,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
        }

    private fun userFlow(companyId: Long, userId: Long) =
        userFlows.computeIfAbsent(companyId to userId) {
            MutableSharedFlow(
                replay = 0,
                extraBufferCapacity = 256,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
        }

    fun flow(companyId: Long): Flow<NotificationResponseDto> =
        companyFlow(companyId).asSharedFlow()

    fun flow(companyId: Long, userId: Long): Flow<NotificationResponseDto> =
        userFlow(companyId, userId).asSharedFlow()

    suspend fun publish(dto: NotificationResponseDto) {
        val cid = dto.companyId
        if (cid == null) {
            log.warn("Skip publish: dto.companyId is null, dtoId={}", dto.id)
            return
        }

        // 회사 전체로 브로드캐스트
        val cf = companyFlow(cid)
        if (!cf.tryEmit(dto)) cf.emit(dto)

        // (선택) 특정 유저 대상 브로드캐스트
        dto.receiverId?.let { uid ->
            val uf = userFlow(cid, uid)
            if (!uf.tryEmit(dto)) uf.emit(dto)
        }
    }

    fun removeCompany(companyId: Long) { companyFlows.remove(companyId) }
    fun removeUser(companyId: Long, userId: Long) { userFlows.remove(companyId to userId) }
}
