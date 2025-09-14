package com.fourka.notification.config.client

interface UserClient {
    suspend fun fetchAdminIds(): Set<Long>
}
