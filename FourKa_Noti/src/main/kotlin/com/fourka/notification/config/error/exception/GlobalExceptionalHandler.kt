package com.fourka.notification.config.error.exception

import com.fourka.notification.config.dto.ApiResponse
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionFailedException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.TransactionSystemException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(this::class.java)

    // 에러 응답 생성 메서드
    private fun buildErrorResponse(errorCode: ErrorCode, errors: Map<String, String>? = null): ApiResponse<Void> {
        return ApiResponse.fail(errorCode, errors ?: emptyMap())
    }

    private fun buildErrorResponse(errorCode: ErrorCode, message: String): ApiResponse<Void> {
        return ApiResponse.fail(errorCode, message)
    }

    private fun buildErrorResponse(errorCode: ErrorCode, field: String, message: String): ApiResponse<Void> {
        val errorMap = mapOf(field to message)
        return buildErrorResponse(errorCode, errorMap)
    }

    @ExceptionHandler(Exception::class)
    suspend fun handleGenericException(ex: Exception): ApiResponse<Void> {
        log.error("Unhandled exception: {}", ex.message, ex)
        return buildErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(CommonException::class)
    suspend fun handleCommonException(ex: CommonException): ApiResponse<Void> {
        val errorCode = ex.errorCode
        return buildErrorResponse(errorCode)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    suspend fun handleValidationExceptions(ex: MethodArgumentNotValidException): ApiResponse<Void> {
        val errors = ex.bindingResult
            .fieldErrors
            .associate { it.field to it.defaultMessage.orEmpty() }
        return buildErrorResponse(ErrorCode.VALIDATION_FAILED, errors)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    suspend fun handleConstraintViolationException(ex: ConstraintViolationException): ApiResponse<Void> {
        val errors = ex.constraintViolations
            .associate { it.propertyPath.toString() to it.message }
        return buildErrorResponse(ErrorCode.VALIDATION_FAILED, errors)
    }

    @ExceptionHandler(TransactionSystemException::class)
    suspend fun handleTransactionSystemException(ex: TransactionSystemException): ApiResponse<Void> {
        val cause = ex.cause
        if (cause is ConstraintViolationException) {
            return handleConstraintViolationException(cause)
        }
        return buildErrorResponse(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.message)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    suspend fun handleMethodArgumentTypeMismatchException(ex: MethodArgumentTypeMismatchException): ApiResponse<Void> {
        val errorMessage = "파라미터 '${ex.name}' 가 적절하지 않은 값을 가지고 있습니다: ${ex.value}"
        return buildErrorResponse(ErrorCode.INVALID_PARAMETER_TYPE, errorMessage)
    }

    @ExceptionHandler(ConversionFailedException::class)
    suspend fun handleConversionFailedException(ex: ConversionFailedException): ApiResponse<Void> {
        val errorMessage = "ENUM '${ex.targetType.type.simpleName}'에 '${ex.value}' 값이 존재하지 않습니다."
        return buildErrorResponse(ErrorCode.INVALID_PARAMETER_TYPE, errorMessage)
    }

    @ExceptionHandler(ServerWebInputException::class)
    suspend fun handleWebInputException(ex: ServerWebInputException): ApiResponse<Void> {
        val errorMessage = "요청 파라미터가 잘못되었거나 누락되었습니다: ${ex.reason}"
        return buildErrorResponse(ErrorCode.INVALID_PARAMETER_TYPE, errorMessage)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    suspend fun handleDataIntegrityViolationException(ex: DataIntegrityViolationException): ApiResponse<Void> {
        return buildErrorResponse(ErrorCode.VALIDATION_ERROR, "필수 입력 필드가 누락되었습니다.")
    }
}
