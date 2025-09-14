package com.fourka.notification.config.error.exception

class CommonException (
    val errorCode: ErrorCode
) : RuntimeException()