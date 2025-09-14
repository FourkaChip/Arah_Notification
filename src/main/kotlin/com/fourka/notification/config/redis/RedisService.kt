package com.fourka.notification.config.redis

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.ReactiveValueOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Duration

@Service
class RedisService(
    @Qualifier("pubSubRedisTemplate")
    private val redisTemplate: ReactiveRedisTemplate<String, String>
) {
    private val ops: ReactiveValueOperations<String, String> = redisTemplate.opsForValue()

    companion object {
        private const val VERIFY_CODE_EXPIRE_MINUTES = 5L
        private const val ATTEMPT_BLOCK_MINUTES = 60L
        private const val REFRESH_TOKEN_EXPIRE_DAYS = 14L

        private const val REFRESH_TOKEN_PREFIX = "refresh:"
        private const val MASTER_VERIFY_PREFIX = "master:verify:"
        private const val MASTER_ATTEMPT_PREFIX = "master:attempt:"
        private const val USER_VERIFY_PREFIX = "user:verify:"
        private const val USER_ATTEMPT_PREFIX = "user:attempt:"
        private const val BLACKLIST_PREFIX = "blacklist:"
    }

    // === 기본 연산 ===

    suspend fun set(key: String, value: String) =
        ops.set(key, value).awaitSingle()

    suspend fun set(key: String, value: String, ttl: Duration) =
        ops.set(key, value, ttl).awaitSingle()

    suspend fun get(key: String): String? =
        ops.get(key).awaitSingleOrNull()

    suspend fun delete(key: String): Boolean =
        redisTemplate.delete(key).awaitSingle() > 0

    suspend fun exists(key: String): Boolean =
        redisTemplate.hasKey(key).awaitSingle()

    // === 블랙리스트 ===

    suspend fun blacklistAccessToken(token: String, expirationMillis: Long) {
        set("$BLACKLIST_PREFIX$token", "logout", Duration.ofMillis(expirationMillis))
    }

    suspend fun isBlacklisted(token: String): Boolean =
        exists("$BLACKLIST_PREFIX$token")

    // === Refresh Token ===

    suspend fun saveRefreshToken(email: String, token: String) =
        set("$REFRESH_TOKEN_PREFIX$email", token, Duration.ofDays(REFRESH_TOKEN_EXPIRE_DAYS))

    suspend fun getRefreshToken(email: String): String? =
        get("$REFRESH_TOKEN_PREFIX$email")

    suspend fun deleteRefreshToken(email: String) =
        delete("$REFRESH_TOKEN_PREFIX$email")

    // === 인증 코드 생성 ===

    fun generateVerifyCode(): String =
        "%06d".format(SecureRandom().nextInt(1_000_000))

    // === Master 인증 코드 ===

    suspend fun sendMasterVerifyCode(email: String) {
        val code = generateVerifyCode()
        set("$MASTER_VERIFY_PREFIX$email", code, Duration.ofMinutes(VERIFY_CODE_EXPIRE_MINUTES))
    }

    suspend fun getMasterVerifyCode(email: String): String? =
        get("$MASTER_VERIFY_PREFIX$email")

    suspend fun deleteMasterVerifyCode(email: String) =
        delete("$MASTER_VERIFY_PREFIX$email")

    suspend fun validateMasterVerifyCode(email: String, input: String): Boolean {
        val stored = getMasterVerifyCode(email)
        return if (stored == null || stored != input) {
            incrementAttempts("$MASTER_ATTEMPT_PREFIX$email")
            false
        } else true
    }

    suspend fun cleanupAfterMasterSuccess(email: String) {
        deleteMasterVerifyCode(email)
        delete("$MASTER_ATTEMPT_PREFIX$email")
    }

    // === User 인증 코드 ===

    suspend fun sendUserVerifyCode(email: String) {
        val code = generateVerifyCode()
        set("$USER_VERIFY_PREFIX$email", code, Duration.ofMinutes(VERIFY_CODE_EXPIRE_MINUTES))
    }

    suspend fun getUserVerifyCode(email: String): String? =
        get("$USER_VERIFY_PREFIX$email")

    suspend fun deleteUserVerifyCode(email: String) =
        delete("$USER_VERIFY_PREFIX$email")

    suspend fun validateUserVerifyCode(email: String, input: String): Boolean {
        val stored = getUserVerifyCode(email)
        return if (stored == null || stored != input) {
            incrementAttempts("$USER_ATTEMPT_PREFIX$email")
            false
        } else true
    }

    suspend fun cleanupAfterUserSuccess(email: String) {
        deleteUserVerifyCode(email)
        delete("$USER_ATTEMPT_PREFIX$email")
    }

    // === 시도 횟수 관련 공통 로직 ===

    private suspend fun incrementAttempts(key: String) {
        val current = get(key)?.toIntOrNull() ?: 0
        set(key, (current + 1).toString(), Duration.ofMinutes(ATTEMPT_BLOCK_MINUTES))
    }

    suspend fun getAttempts(key: String): Int =
        get(key)?.toIntOrNull() ?: 0
}
