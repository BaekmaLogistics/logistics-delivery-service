package com.sparta.logistics.presentation.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String errorCode,
        String message,
        Map<String, String> errors
) {
    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorResponseCode responseCode) {
        return ResponseEntity.status(responseCode.getStatus())
                .body(ErrorResponse.fromData(
                        responseCode, null));
    }

    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorResponseCode responseCode, Map<String, String> errors) {
        return ResponseEntity.status(responseCode.getStatus())
                .body(ErrorResponse.fromData(
                        responseCode, errors));
    }

    private static ErrorResponse fromData(ErrorResponseCode responseCode, Map<String, String> errors) {
        return new ErrorResponse(
                responseCode.getErrorCode(),
                responseCode.getMessage(),
                errors
        );
    }
}
