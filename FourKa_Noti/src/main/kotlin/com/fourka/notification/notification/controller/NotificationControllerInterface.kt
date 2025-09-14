package com.fourka.notification.notification.controller

import com.fourka.notification.config.dto.ApiResponse
import com.fourka.notification.config.security.CustomUserDetails
import com.fourka.notification.notification.dto.response.NotificationListResponseDto
import com.fourka.notification.notification.dto.response.NotificationCountResponseDto
import com.fourka.notification.notification.dto.response.NotificationIdResponseDto
import com.fourka.notification.notification.dto.response.NotificationResponseDto
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.constraints.Min
import kotlinx.coroutines.flow.Flow
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

interface NotificationControllerInterface {

    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "SSE 연결 성공 (text/event-stream)",
                content = [
                    Content(
                        mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                        examples = [
                            ExampleObject(
                                name = "SSE Event",
                                value =
                                // 라인 기반 SSE 예시
                                "id: 123\n" +
                                        "event: notification\n" +
                                        "data: {\n" +
                                        "  \"id\": 123,\n" +
                                        "  \"senderId\": 1,\n" +
                                        "  \"receiverId\": 2,\n" +
                                        "  \"type\": \"FEEDBACK\",\n" +
                                        "  \"content\": {\n" +
                                        "    \"department\": \"영업부\",\n" +
                                        "    \"description\": \"피드백이 작성되었습니다.\"\n" +
                                        "  }\n" +
                                        "}\n\n"
                            )
                        ]
                    )
                ]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증 실패(토큰 누락/유효하지 않음)",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        value = """
                        {
                          "success": false,
                          "code": 401,
                          "timestamp": "2025-08-10T12:00:00",
                          "message": "인증에 실패했습니다.",
                          "error": {}
                        }
                        """
                    )]
                )]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "관리자 인증 실패",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        value = """
                        {
                          "success": false,
                          "code": 500,
                          "timestamp": "2025-08-10T12:00:00",
                          "message": "알림 구독을 실패했습니다.",
                          "error": {}
                        }
                        """
                    )]
                )]
            )
        ]
    )
    fun subscribe(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestHeader(value = "Last-Event-Id", required = false) lastEventId: Long?
    ): Flow<ServerSentEvent<NotificationResponseDto>>

    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "알림 목록 조회 성공",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "성공",
                                      "result": {
                                        "notificationResponseList": [
                                          {
                                            "id": 1,
                                            "createdAt": "2025-08-09T16:01:11.264976",
                                            "senderId": 12,
                                            "receiverId": null,
                                            "type": "FEEDBACK",
                                            "isRead": false,
                                            "content": {
                                              "department": "영업부",
                                              "description": "피드백이 작성되었습니다."
                                            }
                                          }
                                        ]
                                      }
                                    }
                                """
                            )
                        ]
                    )
                ]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "관리자 인증 실패",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": false,
                                      "code": 400,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "잘못된 연결 ID 입니다.",
                                      "error": {}
                                    }
                                """
                            )
                        ]
                    )
                ]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "알림 목록 조회 실패",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": false,
                                      "code": 500,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "알림 목록 조회에 실패했습니다.",
                                      "error": {}
                                    }
                                """
                            )
                        ]
                    )
                ]
            )
        ]
    )
    suspend fun getNotificationList(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestParam(name = "isRead", required = false) isRead: Boolean?,
        @RequestParam(name = "offset", defaultValue = "0") @Min(0) offset: Long,
    ): ApiResponse<NotificationListResponseDto>

    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "안 읽은 알림 개수 조회 성공",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "성공",
                                      "result": {
                                        "count": 5
                                      }
                                    }
                                """
                            )
                        ]
                    )
                ]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "관리자 인증 실패",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": false,
                                      "code": 403,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "유효한 관리자가 아닙니다.",
                                      "error": {}
                                    }
                                """
                            )
                        ]
                    )
                ]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "안 읽은 알림 개수 조회 실패",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": false,
                                      "code": 500,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "안 읽은 알림 개수 조회를 실패했습니다.",
                                      "error": {}
                                    }
                                """
                            )
                        ]
                    )
                ]
            )
        ]
    )
    suspend fun getUnreadNotificationCount(
        @AuthenticationPrincipal user: CustomUserDetails,
    ): ApiResponse<NotificationCountResponseDto>

    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "모든 알림 읽음 처리 성공",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "성공",
                                      "result": {
                                        "count": 5
                                      }
                                    }
                                """
                            )
                        ]
                    )
                ]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "관리자 인증 실패",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": false,
                                      "code": 403,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "유효한 관리자가 아닙니다.",
                                      "error": {}
                                    }
                                """
                            )
                        ]
                    )
                ]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "모든 알림 읽음 처리 실패",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": false,
                                      "code": 500,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "알림 읽기를 실패했습니다.",
                                      "error": {}
                                    }
                                """
                            )
                        ]
                    )
                ]
            )
        ]
    )
    suspend fun markAllAsRead(
        @AuthenticationPrincipal user: CustomUserDetails,
    ): ApiResponse<NotificationCountResponseDto>

    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "알림 읽음 처리 성공",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "성공",
                                      "result": {
                                        "id": 1
                                      }
                                    }
                                """
                            )
                        ]
                    )
                ]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "이미 읽음 처리된 알림",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": false,
                                      "code": 400,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "이미 읽음 처리된 알림입니다.",
                                      "error": {}
                                    }
                                """
                            )
                        ]
                    )
                ]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "관리자 인증 실패",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": false,
                                      "code": 403,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "유효한 관리자가 아닙니다.",
                                      "error": {}
                                    }
                                """
                            )
                        ]
                    )
                ]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "알림 찾기 실패",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": false,
                                      "code": 404,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "알림을 찾을 수 없습니다.",
                                      "error": {}
                                    }
                                """
                            )
                        ]
                    )
                ]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "알림 읽음 처리 실패",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": false,
                                      "code": 500,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "알림 읽기를 실패했습니다.",
                                      "error": {}
                                    }
                                """
                            )
                        ]
                    )
                ]
            )
        ]
    )
    suspend fun markAsRead(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable notificationId: Long
    ): ApiResponse<NotificationIdResponseDto>

    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "마지막으로 읽은 알림 조회 성공",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "성공",
                                      "result": {
                                        "id": 5
                                      }
                                    }
                                """
                            )
                        ]
                    )
                ]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "이미 읽음 처리된 알림",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": false,
                                      "code": 400,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "읽은 알림이 없습니다.",
                                      "error": {}
                                    }
                                """
                            )
                        ]
                    )
                ]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "관리자 인증 실패",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": false,
                                      "code": 403,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "유효한 관리자가 아닙니다.",
                                      "error": {}
                                    }
                                """
                            )
                        ]
                    )
                ]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "알림 읽음 처리 실패",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                value = """
                                    {
                                      "success": false,
                                      "code": 500,
                                      "timestamp": "2025-07-20T00:23:21.9450112",
                                      "message": "알림 읽기를 실패했습니다.",
                                      "error": {}
                                    }
                                """
                            )
                        ]
                    )
                ]
            )
        ]
    )
    suspend fun getMyLastRead(
        @AuthenticationPrincipal user: CustomUserDetails,
    ): ApiResponse<NotificationIdResponseDto>
}
