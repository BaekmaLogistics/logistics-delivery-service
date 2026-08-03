package com.sparta.logistics.presentation.common.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorResponseCode implements ApiResponseCode {
    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"COMMON_0001", "알 수 없는 오류가 발생했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_0002","유효하지 않은 요청입니다."),
    FEIGN_CLIENT_ERROR(HttpStatus.BAD_GATEWAY, "COMMON_0003", "Feign 통신 중 오류가 발생했습니다."),

    // Delivery
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_0001", "배송 정보를 찾을 수 없습니다."),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_0002", "배송경로를 찾을 수 없습니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "DELIVERY_0003", "유효하지 않은 상태 전이 요청입니다."),
    NO_AVAILABLE_DRIVER(HttpStatus.CONFLICT, "DELIVERY_0004", "배정 가능한 배송담당자가 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "DELIVERY_0005", "해당 요청에 대한 권한이 없습니다.");

    private final HttpStatus status;
    private final String errorCode;
    private final String message;
}
