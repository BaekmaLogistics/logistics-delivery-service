package com.sparta.logistics.infrastructure.feign.dto;

import java.util.Map;

public record FeignErrorResponse(
        String errorCode,
        String message,
        Map<String, String> errors
) {
}
