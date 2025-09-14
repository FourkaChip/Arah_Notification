package com.fourka.notification.test.controller

import com.fourka.notification.config.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tests")
@Tag(name = "테스트 API", description = "테스트용 API")
class TestController {

    @GetMapping
    @Operation(summary = "그냥 테스트")
    suspend fun getTest(): ApiResponse<String> =
        ApiResponse.success("테스트 성공!")
}