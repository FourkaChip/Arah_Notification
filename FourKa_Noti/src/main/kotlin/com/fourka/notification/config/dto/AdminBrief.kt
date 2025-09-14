package com.fourka.notification.config.dto

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AdminBrief(
    @JsonAlias("user_id")
    val userId: Long,
    val email: String? = null,
    val name: String? = null,
    val position: String? = null,
    val companyName: String? = null,
    val departmentName: String? = null
)
