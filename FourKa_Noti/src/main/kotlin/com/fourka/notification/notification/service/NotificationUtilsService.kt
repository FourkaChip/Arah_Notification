package com.fourka.notification.notification.service

import com.fourka.notification.config.security.CustomUserDetails
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Service

@Service
class NotificationUtilsService {
    suspend fun getCurrentUserDetail(): CustomUserDetails {
        val auth = ReactiveSecurityContextHolder.getContext().awaitFirstOrNull()?.authentication
        val user = auth?.principal as CustomUserDetails

        return user
    }
}
