package com.sparta.logistics.infrastructure.feign.service;

import com.sparta.logistics.infrastructure.feign.client.HubFeignClient;
import com.sparta.logistics.infrastructure.feign.dto.HubRoutePageResponse;
import com.sparta.logistics.common.code.ErrorResponseCode;
import com.sparta.logistics.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Hub 서비스의 허브 연결(구간) 정보 조회를 캐싱하는 래퍼.
 * 허브 간 거리/시간은 관리자가 수정하지 않는 이상 거의 바뀌지 않는 데이터라서,
 * 배송 생성마다 매번 Hub 서비스를 호출하는 대신 Redis에 캐싱해서 재사용한다.
 * (RedisConfig의 cacheManager 기본 TTL 10분 적용)
 */
// @Cacheable 같은 어노테이션은 실제로 "프록시"를 통해서 동작.
// 프록시는 "클래스 바깥에서 호출할 때만" 작동하므로
// 같은 클래스의 다른 메서드(buildRoutes)에서 this.getHubRoute(...)처럼 직접 호출하면,
// 프록시를 거치지 않고 진짜 객체를 직접 호출하는 거라 캐싱이 아예 작동을 안 함. (self-invocation 문제)
// 따라서 캐싱이 필요한 메서드는 반드시 별도의 빈(클래스)으로 분리
// 다른 클래스에서 주입받아 호출하는 형태 -> 프록시가 제대로 끼어들 수 있음.
@Component
@RequiredArgsConstructor
public class HubRouteQueryService {

    private final HubFeignClient hubFeignClient;

    @Cacheable(value = "hubRoute", key = "#fromHubId + ':' + #toHubId")
    public HubRoutePageResponse.HubRouteItem getHubRoute(UUID fromHubId, UUID toHubId) {
        HubRoutePageResponse response = hubFeignClient.getHubRoutes(fromHubId, toHubId).data();

        return response.content().stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorResponseCode.HUB_ROUTE_NOT_FOUND));
    }
}
