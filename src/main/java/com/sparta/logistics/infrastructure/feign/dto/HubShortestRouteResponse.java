package com.sparta.logistics.infrastructure.feign.dto;

import java.util.List;
import java.util.UUID;

/**
 * Hub 서비스의 "허브 간 최적 경로 조회"(GET /api/v1/hub-routes/shortest) 응답 DTO.
 * 경유하는 허브들의 순서(routes)와 전체 거리/시간(total*)만 제공하고,
 * 구간별(허브-허브 사이) 거리/시간은 별도로 HubRoutePageResponse를 통해 조회해야 한다.
 */
public record HubShortestRouteResponse(
        Double totalDistance,
        Integer totalDuration,
        List<HubWaypoint> routes
) {
    public record HubWaypoint(
            UUID hubId,
            String hubName
    ) {
    }
}
