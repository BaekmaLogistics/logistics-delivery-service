package com.sparta.logistics.application.query.dto;

import com.sparta.logistics.domain.entity.DeliveryRoute;
import com.sparta.logistics.domain.model.RouteStatus;

import java.util.UUID;

/**
 * 배송 구간(DeliveryRoute) 응답 DTO.
 * - 배송 상세 조회(DeliveryDetailResponse.routes)
 * - 구간 목록 조회(GET .../routes)
 * - 구간 상태 수정 응답(PATCH .../routes/{routeId}/status)
 * 세 군데에서 공통으로 쓰인다.
 */
public record RouteResponse(
        UUID id,
        Integer sequence,
        UUID fromHubId,
        UUID toHubId,
        Double expectedDistance,
        Integer expectedDuration,
        Double actualDistance,
        Integer actualDuration,
        RouteStatus status,
        UUID driverId
) {
    public static RouteResponse from(DeliveryRoute route) {
        return new RouteResponse(
                route.getId(),
                route.getSequence(),
                route.getFromHubId(),
                route.getToHubId(),
                route.getExpectedDistance(),
                route.getExpectedDuration(),
                route.getActualDistance(),
                route.getActualDuration(),
                route.getStatus(),
                route.getDriverId()
        );
    }
}
