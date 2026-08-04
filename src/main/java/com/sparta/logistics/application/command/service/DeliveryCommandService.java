package com.sparta.logistics.application.command.service;

import com.sparta.logistics.application.command.dto.CreateDeliveryRequest;
import com.sparta.logistics.application.command.dto.DeliveryResponse;
import com.sparta.logistics.application.command.usecase.CreateDeliveryUseCase;
import com.sparta.logistics.domain.entity.Delivery;
import com.sparta.logistics.domain.entity.DeliveryRoute;
import com.sparta.logistics.domain.repository.DeliveryRepository;
import com.sparta.logistics.infrastructure.feign.client.HubFeignClient;
import com.sparta.logistics.infrastructure.feign.client.UserFeignClient;
import com.sparta.logistics.infrastructure.feign.constant.DeliveryManagerType;
import com.sparta.logistics.infrastructure.feign.dto.DeliveryManagerPageResponse;
import com.sparta.logistics.infrastructure.feign.dto.HubRoutePageResponse;
import com.sparta.logistics.infrastructure.feign.dto.HubShortestRouteResponse;
import com.sparta.logistics.infrastructure.feign.service.HubRouteQueryService;
import com.sparta.logistics.presentation.common.dto.response.ErrorResponseCode;
import com.sparta.logistics.presentation.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryCommandService implements CreateDeliveryUseCase {

    private final DeliveryRepository deliveryRepository;
    private final HubFeignClient hubFeignClient;
    private final HubRouteQueryService hubRouteQueryService;
    private final UserFeignClient userFeignClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public DeliveryResponse createDelivery(CreateDeliveryRequest request) {
        // 배송 전체를 담당할 업체배송담당자를 먼저 정함 (User&Auth 호출)
        UUID companyDriverId = assignCompanyDriver(request.destinationHubId());

        // 부모 엔티티를 메모리상에 생성 (아직 DB엔 안 들어감, status는 자동으로 HUB_WAITING)
        Delivery delivery = Delivery.create(
                request.orderId(),
                request.departureHubId(),
                request.destinationHubId(),
                request.deliveryAddress(),
                request.receiverName(),
                request.receiverSlackId(),
                companyDriverId
        );

        // 구간들을 계산해서 하나씩 부모에 붙임.
        // delivery::addRoute는 route -> delivery.addRoute(route)를 줄인 메서드 레퍼런스
        buildRoutes(request.departureHubId(), request.destinationHubId())
                .forEach(delivery::addRoute);

        // cascade = ALL 덕분에 delivery + 딸린 route들이 한꺼번에 INSERT됨
        Delivery savedDelivery = deliveryRepository.save(delivery);

        // 저장된(ID 채워진) 엔티티를 응답 DTO로 변환
        return DeliveryResponse.from(savedDelivery);
    }

    /**
     * Hub 서비스에 최적 경로를 물어본 뒤(경유 허브 순서), 연속된 두 허브씩 짝지어서
     * 구간별 거리/시간을 조회하고 DeliveryRoute 목록을 만든다.
     */
    private List<DeliveryRoute> buildRoutes(UUID departureHubId, UUID destinationHubId) {
        // Hub한테 출발허브에서 도착허브까지 최적경로가 뭔지 물어봄.
        HubShortestRouteResponse shortestRoute =
                hubFeignClient.getShortestRoute(departureHubId, destinationHubId).data();

        List<HubShortestRouteResponse.HubWaypoint> waypoints = shortestRoute.routes();

        // 허브가 3개(서울, 대전, 부산)면 구간은 2개(서울→대전, 대전→부산).
        // 그래서 인덱스를 0부터 허브개수-2까지(waypoints.size()-1개) 돌림.
        // i=0일 때 (서울,대전), i=1일 때 (대전,부산) 이런 식으로 짝지어짐.
        return IntStream.range(0, waypoints.size() - 1)
                // IntStream(정수 스트림)을 Stream<DeliveryRoute>(객체 스트림)로 변환
                .mapToObj(i -> {
                    // 연속된 두 허브를 뽑아냄 (구간의 출발/도착)
                    UUID fromHubId = waypoints.get(i).hubId();
                    UUID toHubId = waypoints.get(i + 1).hubId();
                    int sequence = i + 1; // 이해하기 쉽게 1부터 시작

                    // 캐싱 래퍼를 호출 -> 이 구간의 실제 거리/시간을 받아옴
                    // 캐시에 있으면 Redis에서, 없으면 Hub 서비스 호출 후 캐시에 저장
                    HubRoutePageResponse.HubRouteItem hubRoute =
                            hubRouteQueryService.getHubRoute(fromHubId, toHubId);

                    // 이 구간을 담당할 기사를 정함
                    UUID segmentDriverId = assignHubDriver(fromHubId);

                    return DeliveryRoute.create(
                            sequence,
                            fromHubId,
                            toHubId,
                            hubRoute.distance(),
                            hubRoute.duration(),
                            segmentDriverId
                    );
                })
                // 스트림을 최종적으로 List<DeliveryRoute>로 모음.
                .toList();
    }

    /**
     * 목적지 허브 소속 업체배송담당자를 순번 기준으로 순환 배정(round-robin)한다.
     */
    private UUID assignCompanyDriver(UUID destinationHubId) {
        return findDeliveryManager(DeliveryManagerType.COMPANY_DELIVERY, destinationHubId);
    }

    private UUID assignHubDriver(UUID hubId) {
        return findDeliveryManager(DeliveryManagerType.HUB_DELIVERY, hubId);
    }

    /**
     * User&Auth 서비스에서 (deliveryType, hubId) 조건에 맞는 담당자 목록을 받아
     * deliveryOrder 순으로 정렬한 뒤, Redis INCR로 순환(round-robin) 배정한다.
     * INCR은 원자적 연산이라, 동시에 여러 배송이 생성돼도 카운터가 겹치지 않고
     * 하나씩 정확히 증가한다 -> 배정도 겹치지 않고 순서대로 돌아가며 이루어짐.
     */
    private UUID findDeliveryManager(String deliveryType, UUID hubId) {
        DeliveryManagerPageResponse response =
                userFeignClient.getDeliveryManagers(deliveryType, hubId).data();

        List<DeliveryManagerPageResponse.DeliveryManagerItem> managers = response.content().stream()
                .sorted((a, b) -> Integer.compare(a.deliveryOrder(), b.deliveryOrder()))
                .toList();

        if (managers.isEmpty()) {
            throw new ApiException(ErrorResponseCode.NO_AVAILABLE_DRIVER);
        }

        String counterKey = "delivery-manager-rr::" + deliveryType + ":" + hubId;
        // increment()는 키가 없으면 0에서 시작해 1을 리턴, 그 다음엔 2, 3, ... 계속 증가.
        long count = redisTemplate.opsForValue().increment(counterKey);
        // count는 1부터 시작하므로 -1 해서 0-based 인덱스로 변환 후, 목록 크기로 나눈 나머지를 인덱스로 사용
        // -> 0, 1, 2, ..., size-1, 0, 1, ... 순서로 계속 순환됨
        int index = (int) ((count - 1) % managers.size());

        return managers.get(index).userId();
    }
}
