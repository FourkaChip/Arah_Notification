package com.fourka.notification.config.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fourka.notification.config.error.exception.ErrorCode
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val code: Int,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val message: String,
    val result: T? = null,
    val error: Map<String, String>? = null
) {
    companion object {
        /**
         * 성공 응답 (커스텀 코드 및 메시지)
         */
        fun <T> success(
            result: T? = null,
            code: Int = 200,
            message: String = "성공"
        ): ApiResponse<T> = ApiResponse(
            success = true,
            code = code,
            message = message,
            result = result
        )

        fun <T> create(
            result: T? = null,
            code: Int = 201,
            message: String = "생성 성공"
        ): ApiResponse<T> = ApiResponse(
            success = true,
            code = code,
            message = message,
            result = result
        )

        /**
         * 실패 응답 (단일 필드 에러)
         */
        fun <T> fail(
            code: ErrorCode,
            field: String,
            errorMessage: String
        ): ApiResponse<T> = fail(
            code,
            errorMap = mapOf(field to errorMessage)
        )

        /**
         * 실패 응답 (다중 필드 에러)
         */
        fun <T> fail(
            code: ErrorCode,
            errorMap: Map<String, String>
        ): ApiResponse<T> = ApiResponse(
            success = false,
            code = code.status,
            message = code.message,
            error = errorMap
        )

        /**
         * 실패 응답 (메시지 커스텀)
         */
        fun <T> fail(
            code: ErrorCode,
            message: String
        ): ApiResponse<T> = ApiResponse(
            success = false,
            code = code.status,
            message = message
        )

        /**
         * 실패 응답 (기본)
         */
        fun <T> fail(
            code: ErrorCode
        ): ApiResponse<T> = ApiResponse(
            success = false,
            code = code.status,
            message = code.message
        )
    }
}
