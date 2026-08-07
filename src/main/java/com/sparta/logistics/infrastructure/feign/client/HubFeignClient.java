package com.sparta.logistics.infrastructure.feign.client;

import com.sparta.logistics.infrastructure.feign.dto.HubRoutePageResponse;
import com.sparta.logistics.infrastructure.feign.dto.HubShortestRouteResponse;
import com.sparta.logistics.presentation.common.dto.response.GeneralResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Hub 서비스(logistics-hub-service) Feign 클라이언트.
 * name은 Eureka에 등록된 spring.application.name 값과 일치해야 한다.
 */
// hub-service라는 이름으로 등록된 인스턴스 IP/포트를 Eureka에 물어보고
// 그 주소로 실제 HTTP 요청 대신 보냄.
@FeignClient(name = "hub-service")
public interface HubFeignClient {

    /**
     * 허브 간 최적 경로 조회. 경유 허브 순서 + 전체 거리/시간만 내려온다.
     */
    @GetMapping("/internal/api/v1/hub-routes/shortest")
    GeneralResponse<HubShortestRouteResponse> getShortestRoute(
            @RequestParam("fromHubId") UUID fromHubId,
            @RequestParam("toHubId") UUID toHubId
    );

    /**
     * 허브 연결(직통 구간) 목록 조회. fromHubId+toHubId를 모두 넘기면 해당 구간 1건만 조회됨.
     */
    @GetMapping("/internal/api/v1/hub-routes")
    GeneralResponse<HubRoutePageResponse> getHubRoutes(
            @RequestParam("fromHubId") UUID fromHubId,
            @RequestParam("toHubId") UUID toHubId
    );
}
