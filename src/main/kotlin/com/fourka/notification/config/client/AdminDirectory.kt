package com.fourka.notification.config.client

interface AdminDirectory {
    suspend fun isAdmin(userId: Long): Boolean
    suspend fun refresh(): Set<Long>
}
