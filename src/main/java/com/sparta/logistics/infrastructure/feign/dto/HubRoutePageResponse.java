package com.sparta.logistics.infrastructure.feign.dto;

import java.util.List;
import java.util.UUID;

/**
 * Hub 서비스의 "허브 연결 목록 조회"(GET /api/v1/hub-routes) 응답 DTO.
 * fromHubId + toHubId를 둘 다 지정해서 호출하면 content에 정확히 1건(해당 직통 구간)이 담겨온다.
 * 주의: Hub 서비스는 페이지 필드명이 totalPages(복수형)이다. User 서비스의 totalPage(단수형)와 다르니 헷갈리지 말 것.
 */
public record HubRoutePageResponse(
        List<HubRouteItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public record HubRouteItem(
            UUID id,
            UUID fromHubId,
            UUID toHubId,
            Double distance,
            Integer duration
    ) {
    }
}
