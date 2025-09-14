package com.fourka.notification.config.error.exception

data class ErrorResponse(
    val status: Int,
    val code: String,
    val message: String
) {
    companion object {
        fun of(errorCode: ErrorCode): ErrorResponse =
            ErrorResponse(
                status = errorCode.status,
                code = errorCode.code,
                message = errorCode.message
            )
    }
}