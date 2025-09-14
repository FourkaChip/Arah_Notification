package com.fourka.notification.config.client

import com.fourka.notification.config.error.exception.CommonException
import com.fourka.notification.config.error.exception.ErrorCode
import org.springframework.stereotype.Component

@Component
class AdminVerifier(
    private val adminDirectory: AdminDirectory
) {
    suspend fun assertAdmin(userId: Long) {
        if (!adminDirectory.isAdmin(userId)) {
            throw CommonException(ErrorCode.UNAUTHORIZED_ADMIN)
        }
    }
}
