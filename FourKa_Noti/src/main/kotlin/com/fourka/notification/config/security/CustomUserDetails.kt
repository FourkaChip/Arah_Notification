package com.fourka.notification.config.security

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class CustomUserDetails(
    private val userId: Long,
    private val email: String,
    private val role: String,
    private val companyId: Long,
) : UserDetails {
    override fun getAuthorities() = listOf(SimpleGrantedAuthority("ROLE_$role"))
    override fun getPassword() = ""
    override fun getUsername() = email
    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = true

    fun getUserId() = userId // 커스텀 필드
    fun getCompanyId() = companyId
}
