package com.sparta.logistics.infrastructure.feign.dto;

import java.util.List;
import java.util.UUID;

/**
 * Hub 서비스의 "허브 간 최적 경로 조회"(GET /internal/api/v1/hub-routes/shortest) 응답 DTO.
 * 경유하는 허브 순서(hubIds)뿐 아니라 구간별(허브-허브 사이) 거리/시간(segments)과
 * 전체 거리/시간(total*)까지 한 번에 내려온다 - 예전엔 구간별 거리/시간을 알려면
 * /hub-routes를 구간마다 따로 호출해야 했는데, 이 응답 하나로 다 해결된다.
 * hubIds는 route 생성 로직에서 직접 쓰이진 않지만(segments만으로 충분), 응답에 포함되어 있어 남겨둔다.
 */
public record HubShortestRouteResponse(
        List<UUID> hubIds,
        List<Segment> segments,
        Double totalDistance,
        Integer totalDuration
) {
    public record Segment(
            UUID fromHubId,
            UUID toHubId,
            Double distance,
            Integer duration
    ) {
    }
}
