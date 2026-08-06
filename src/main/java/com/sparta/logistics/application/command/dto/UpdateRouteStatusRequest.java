package com.sparta.logistics.application.command.dto;

import com.sparta.logistics.domain.model.RouteStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 구간(route) 상태 변경 요청 DTO.
 * status는 현재 상태의 "바로 다음" 상태만 허용된다 (건너뛰기/역행 불가).
 * status가 ROUTE_ARRIVED일 때만 actualDistance/actualDuration이 필수 - 그 외에는 무시된다.
 */
public record UpdateRouteStatusRequest(
        @NotNull RouteStatus status,
        Double actualDistance,
        Integer actualDuration
) {
}
