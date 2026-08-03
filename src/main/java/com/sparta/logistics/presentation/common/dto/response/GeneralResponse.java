package com.sparta.logistics.presentation.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.ResponseEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeneralResponse<T>(
        String message,
        T data
) {
    public static <T> ResponseEntity<GeneralResponse<T>> toResponseEntity(ApiResponseCode responseCode, T data) {
        return ResponseEntity.status(responseCode.getStatus())
                .body(fromData(responseCode, data));
    }

    private static <T> GeneralResponse<T> fromData(ApiResponseCode responseCode, T data) {
        return new GeneralResponse<>(responseCode.getMessage(), data);
    }
}