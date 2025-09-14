package com.fourka.notification.config.client

import com.fourka.notification.config.error.exception.CommonException
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

// 관리자 검증을 네트워크 호출 없이 처리하려고 Redis SET + 간이 락으로 감싼 어댑터
@Component
class ReactiveAdminDirectory(
    @Qualifier("pubSubRedisTemplate")
    private val redis: ReactiveRedisTemplate<String, String>,
    private val userClient: UserClient
) : AdminDirectory {

    private val key = "cache:admin-ids" // 관리자 userId 들을 Set으로 저장
    private val lockKey = "cache:admin-ids:lock"
    private val ttl: Duration = Duration.ofSeconds(120)
    private val lockTtl: Duration = Duration.ofSeconds(5)

    override suspend fun isAdmin(userId: Long): Boolean {
        // 캐시 히트면 SISMEMBER 로 즉시 확인 (논블로킹)
        val hasKey = redis.hasKey(key).awaitSingle()
        if (hasKey) {
            return redis.opsForSet().isMember(key, userId.toString()).awaitSingle() ?: false
        }

        // 캐시 미스면 스탬피드 방지를 위해 분산 락 시도
        if (tryAcquireLock()) {
            try {
                val ids = fetchAndFill() // 원격(UserClient) 호출 → 캐시 채움
                return userId in ids
            } finally {
                redis.delete(lockKey).awaitSingleOrNull()
            }
        } else {
            delay(150) // // 누가 채우는 중이면 잠깐 대기
            return redis.opsForSet().isMember(key, userId.toString()).awaitSingle() ?: false
        }
    }

    // 리프레쉬를 통한 최신화
    // Todo - 관리자 리스트 변경 트리거 발생 시 refresh 하도록 개선하면 좋음
    override suspend fun refresh(): Set<Long> = fetchAndFill()

    private suspend fun tryAcquireLock(): Boolean =
        redis.opsForValue().setIfAbsent(lockKey, "1", lockTtl).awaitSingle() ?: false

    // 강제로 원격 조회 → 캐시 교체
    private suspend fun fetchAndFill(): Set<Long> =
        try {
            val ids = userClient.fetchAdminIds()
            redis.delete(key).awaitSingleOrNull()
            if (ids.isNotEmpty()) {
                val values = ids.map(Long::toString).toTypedArray()
                redis.opsForSet().add(key, *values).awaitSingleOrNull()
                redis.expire(key, ttl).awaitSingle()
            }
            ids
        } catch (e: CommonException) {
            throw e // 권한 오류는 그대로 전파
        } catch (e: Exception) {
            emptySet()
        }
}
