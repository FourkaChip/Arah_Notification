package com.fourka.notification.config.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.Key
import java.util.Base64
import java.util.Date
import javax.crypto.spec.SecretKeySpec

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secretKey: String,
    @Value("\${jwt.access-expiration}") private val accessExpiration: Long,
    @Value("\${jwt.refresh-expiration}") private val refreshExpiration: Long,
    @Value("\${jwt.reset-password-expiration}") private val resetPasswordExpiration: Long
) {
    private val secret: Key = SecretKeySpec(
        Base64.getDecoder().decode(secretKey.toByteArray(StandardCharsets.UTF_8)),
        SignatureAlgorithm.HS512.jcaName
    )


    fun createAccessToken(email: String, userId: Long, role: String, companyId: Long): String {
        val claims = Jwts.claims().setSubject(email)
        claims["user_id"] = userId
        claims["role"] = role
        claims["company_id"] = companyId

        val now = Date()
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(Date(now.time + accessExpiration * 60 * 1000))
            .signWith(secret)
            .compact()
    }

    fun createRefreshToken(email: String, userId: Long, role: String, companyId: Long): String {
        val claims = Jwts.claims().setSubject(email)
        claims["user_id"] = userId
        claims["role"] = role
        claims["company_id"] = companyId

        val now = Date()
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(Date(now.time + refreshExpiration * 60 * 1000))
            .signWith(secret)
            .compact()
    }

    // === 클레임 추출 ===

    fun extractClaims(token: String): Claims {
        val claims = Jwts.parserBuilder()
            .setSigningKey(secret)
            .build()
            .parseClaimsJws(token)
            .body
        println(claims)
        return claims
    }
/*        Jwts.parserBuilder()
            .setSigningKey(secret)
            .build()
            .parseClaimsJws(token)
            .body*/

    fun extractBearerToken(header: String): String =
        header.removePrefix("Bearer ")

    fun getEmailFromToken(token: String): String =
        extractClaims(token).subject

    fun getUserIdFromToken(token: String): Long =
        extractClaims(token)["user_id", Long::class.java]

    fun getCompanyIdFromToken(token: String): Long =
        extractClaims(token)["company_id", Long::class.java]

    fun getRoleFromToken(token: String): String =
        extractClaims(token)["role", String::class.java]

    // === 유효성 검증 ===

    fun validateToken(token: String): Boolean = try {
        val claims = extractClaims(token)
        !claims.expiration.before(Date())
    } catch (e: ExpiredJwtException) {
        false
    } catch (e: JwtException) {
        false
    }

    // === 만료 시간 계산 ===

    fun getExpiration(token: String): Long {
        val claims = extractClaims(token)
        return claims.expiration.time - System.currentTimeMillis()
    }

    fun getRefreshExpiration(): Long = refreshExpiration

    fun parseToken(token: String): CustomUserDetails {
        // 1. JWT 파싱/유효성 검증
        // 2. 클레임에서 userId, email, role 등 꺼내서 CustomUserDetails 직접 생성
        // 예시:
        val claims = Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .body
        val userId = claims["user_id"].toString().toLong()
        val companyId = claims["company_id"].toString().toLong()
        val email = claims["sub"].toString()
        val role = claims["role"].toString()
        return CustomUserDetails(userId, email, role, companyId)
    }
}
