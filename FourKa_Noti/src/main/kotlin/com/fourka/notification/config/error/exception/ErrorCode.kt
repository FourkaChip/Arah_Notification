package com.fourka.notification.config.error.exception

enum class ErrorCode(
    val status: Int,
    val code: String,
    val message: String
) {
    // 공통
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "서버 내부 에러"),
    VALIDATION_FAILED(400, "VALIDATION_FAILED", "요청 값이 올바르지 않습니다."),
    JSON_PARSING_ERROR(400, "JSON_PARSING_ERROR", "JSON 데이터 처리 중 오류가 발생했습니다"),
    INVALID_PARAMETER_TYPE(400, "INVALID_PARAMETER_TYPE", "적절하지 않은 파라미터 타입입니다."),
    VALIDATION_ERROR(400, "VALIDATION_ERROR", "유효성 검사 오류입니다."),
    INVALID_REQUEST_FORMAT(400, "INVALID_REQUEST_FORMAT", "올바르지 않은 요청 형식입니다."),
    NO_RESOURCE_FOUND(404, "NO_RESOURCE_FOUND", "해당 리소스를 찾을 수 없습니다."),
    UNSUPPORTED_MEDIA_TYPE(415, "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 미디어 타입입니다."),

    // 회원
    INVALID_TOKEN(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    USER_NOT_FOUND(404, "USER_NOT_FOUND", "유저를 찾을 수 없습니다."),
    TOKEN_NOT_FOUND(404, "TOKEN_NOT_FOUND", "토큰을 찾을 수 없습니다."),
    MEMBER_ALREADY_EXISTS(409, "MEMBER_ALREADY_EXISTS", "이미 가입된 이메일입니다."),
    INVALID_LOGIN_INFO(401, "INVALID_LOGIN_INFO", "로그인 정보가 올바르지 않습니다."),
    UNAUTHORIZED_ADMIN(403, "UNAUTHORIZED_ADMIN", "유효한 관리자가 아닙니다."),

    // 알림
    SENDER_ID_NULL(400, "SENDER_ID_NULL", "발신자 ID 값이 누락되었습니다."),
    NOTIFICATION_ID_NULL(400, "NOTIFICATION_ID_NULL", "알림 ID 값이 누락되었습니다."),
    NOTIFICATION_TYPE_NOY_EXIST(400, "NOTIFICATION_TYPE_NOY_EXIST", "존재하지 않는 타입입니다."),
    FAIL_SEND_NOTIFICATION(400, "FAIL_SEND_NOTIFICATION", "알림 전송을 실패했습니다."),
    FAIL_FETCH_NOTIFICATION_LIST(500, "FAIL_FETCH_NOTIFICATION_LIST", "알림 목록 조회를 실패했습니다."),
    FAIL_FETCH_UNREAD_NOTIFICATION_COUNT(500, "FAIL_FETCH_UNREAD_NOTIFICATION_COUNT", "안 읽은 알림 개수 조회를 실패했습니다."),
    FAIL_READ_NOTIFICATION(500, "FAIL_READ_NOTIFICATION", "알림 읽기를 실패했습니다."),
    NO_UNREAD_NOTIFICATIONS(400, "NO_UNREAD_NOTIFICATIONS", "읽을 알림이 없습니다."),
    NOTIFICATION_NOT_FOUND(404, "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."),
    ALREADY_READ_NOTIFICATION(400, "ALREADY_READ_NOTIFICATION", "이미 읽음 처리된 알림입니다."),
    NO_READ_NOTIFICATIONS(400, "NO_READ_NOTIFICATIONS", "읽은 알림이 없습니다."),
    FAIL_FETCH_LAST_READ_NOTIFICATION(500, "FAIL_FETCH_LAST_READ_NOTIFICATION", "마지막으로 읽은 알림 조회를 실패했습니다."),
    COMPANY_ID_NULL(400, "COMPANY_ID_NULL", "회사 ID 값이 누락되었습니다."),

    // 테스트
    TEST_ERROR_STATUS(400, "TEST_ERROR", "테스트용 에러입니다."),
    ;

}
