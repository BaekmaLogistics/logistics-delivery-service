package com.sparta.logistics.application.command.service;

import com.sparta.logistics.application.command.dto.CancelDeliveryRequest;
import com.sparta.logistics.application.command.dto.CreateDeliveryRequest;
import com.sparta.logistics.application.command.dto.DeliveryResponse;
import com.sparta.logistics.application.command.dto.UpdateDeliveryStatusRequest;
import com.sparta.logistics.application.command.dto.UpdateRouteStatusRequest;
import com.sparta.logistics.application.command.usecase.CancelDeliveryUseCase;
import com.sparta.logistics.application.command.usecase.CreateDeliveryUseCase;
import com.sparta.logistics.application.command.usecase.DeleteDeliveryUseCase;
import com.sparta.logistics.application.command.usecase.UpdateDeliveryStatusUseCase;
import com.sparta.logistics.application.command.usecase.UpdateRouteStatusUseCase;
import com.sparta.logistics.application.query.dto.RouteResponse;
import com.sparta.logistics.common.constant.UserRole;
import com.sparta.logistics.domain.entity.Delivery;
import com.sparta.logistics.domain.entity.DeliveryRoute;
import com.sparta.logistics.domain.model.DeliveryStatus;
import com.sparta.logistics.domain.model.RouteStatus;
import com.sparta.logistics.domain.repository.DeliveryRepository;
import com.sparta.logistics.infrastructure.feign.client.HubFeignClient;
import com.sparta.logistics.infrastructure.feign.client.UserFeignClient;
import com.sparta.logistics.infrastructure.feign.constant.DeliveryManagerType;
import com.sparta.logistics.infrastructure.feign.dto.DeliveryManagerPageResponse;
import com.sparta.logistics.infrastructure.feign.dto.HubShortestRouteResponse;
import com.sparta.logistics.common.code.ErrorResponseCode;
import com.sparta.logistics.common.exception.ApiException;
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
public class DeliveryCommandService implements
        CreateDeliveryUseCase, UpdateDeliveryStatusUseCase, UpdateRouteStatusUseCase, DeleteDeliveryUseCase,
        CancelDeliveryUseCase {

    private final DeliveryRepository deliveryRepository;
    private final HubFeignClient hubFeignClient;
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
     * 배송 상태를 다음 단계로 전이한다.
     * - 권한 : COMPANY_MANAGER는 아예 불가(FORBIDDEN). DELIVERY_DRIVER는 본인 담당(Delivery.isAssignedTo)
     *   건이 아니면 FORBIDDEN. MASTER/HUB_MANAGER(임시 무제한, TODO: 담당 허브로 제한)는 통과.
     * - 상태 전이 : 현재 상태의 "바로 다음" 상태만 허용(enum ordinal 기준). 건너뛰거나 역행하면
     *   INVALID_STATUS_TRANSITION. DELIVERED(마지막 상태)에서는 그 다음이 없으므로 항상 거부된다.
     */
    @Override
    public DeliveryResponse updateStatus(
            UUID deliveryId,
            UpdateDeliveryStatusRequest request,
            UUID currentUserId,
            UserRole role
    ) {
        // id로 배송 조회(삭제된 건 자동 제외)
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new ApiException(ErrorResponseCode.DELIVERY_NOT_FOUND));

        // 권한 체크
        validateStatusUpdateAccess(delivery, currentUserId, role);
        // 상태 전이가 유효한지 체크
        validateStatusTransition(delivery.getStatus(), request.status());

        delivery.changeStatus(request.status());

        // 트랜잭션 안에서 영속 상태인 엔티티라 save() 호출 없이도 커밋 시 변경분이 반영된다(더티체킹).
        return DeliveryResponse.from(delivery);
    }

    // 권한 체크
    private void validateStatusUpdateAccess(Delivery delivery, UUID currentUserId, UserRole role) {
        if (role == UserRole.COMPANY_MANAGER) {
            // 배송 "수정"은 업체 담당자는 아예 불가 (X)
            throw new ApiException(ErrorResponseCode.FORBIDDEN);
        }

        if (role == UserRole.DELIVERY_DRIVER && !delivery.isAssignedTo(currentUserId)) {
            throw new ApiException(ErrorResponseCode.FORBIDDEN);
        }
    }

    // 상태 전이 체크
    private void validateStatusTransition(DeliveryStatus current, DeliveryStatus next) {
        if (next.ordinal() != current.ordinal() + 1) {
            throw new ApiException(ErrorResponseCode.INVALID_STATUS_TRANSITION);
        }
    }

    /**
     * 배송 구간(route) 상태를 다음 단계로 전이한다.
     * - 권한 : COMPANY_MANAGER는 아예 불가(FORBIDDEN). DELIVERY_DRIVER는 이 "구간"의 담당자
     *   (route.driverId)가 본인이 아니면 FORBIDDEN. 배송 전체 담당(companyDriverId)이어도
     *   그 사람이 이 구간(허브 간 이동)까지 담당하는 건 아니므로, Issue #7의 isAssignedTo()
     *   (전체 배송 기준)와 달리 여기서는 route.driverId만 본다. MASTER/HUB_MANAGER(임시 무제한)는 통과.
     * - 상태 전이 : 현재 상태의 "바로 다음" 상태만 허용(enum ordinal 기준).
     * - ROUTE_ARRIVED로 전이할 때는 실제 거리/소요시간이 같이 와야 하고, DeliveryRoute.completeRoute()로 기록.
     *   그 외 상태는 DeliveryRoute.changeStatus()만 호출.
     */
    @Override
    public RouteResponse updateRouteStatus(
            UUID deliveryId,
            UUID routeId,
            UpdateRouteStatusRequest request,
            UUID currentUserId,
            UserRole role
    ) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new ApiException(ErrorResponseCode.DELIVERY_NOT_FOUND));

        // 배송을 통째로 조회(routes 포함), 그 안에서 routeId가 일치하는 구간을 찾음
        DeliveryRoute route = delivery.getRoutes().stream()
                .filter(r -> r.getId().equals(routeId) && r.getDeletedAt() == null)
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorResponseCode.ROUTE_NOT_FOUND));

        // 권한 체크
        validateRouteStatusUpdateAccess(route, currentUserId, role);

        // 상태 전이 순서 체크
        validateRouteStatusTransition(route.getStatus(), request.status());

        // ROUTE_ARRIVED면 실거리/실시간 필수 체크 후 completeRoute()
        // 아니면 그냥 changeStatus()
        if (request.status() == RouteStatus.ROUTE_ARRIVED) {
            validateCompleteRouteInput(request);
            route.completeRoute(request.actualDistance(), request.actualDuration());
        } else {
            route.changeStatus(request.status());
        }

        return RouteResponse.from(route);
    }

    // 권한 체크
    private void validateRouteStatusUpdateAccess(DeliveryRoute route, UUID currentUserId, UserRole role) {
        if (role == UserRole.COMPANY_MANAGER) {
            throw new ApiException(ErrorResponseCode.FORBIDDEN);
        }

        if (role == UserRole.DELIVERY_DRIVER && !route.getDriverId().equals(currentUserId)) {
            throw new ApiException(ErrorResponseCode.FORBIDDEN);
        }
    }

    // enum 순서번호로 바로 다음 상태인지 확인
    private void validateRouteStatusTransition(RouteStatus current, RouteStatus next) {
        if (next.ordinal() != current.ordinal() + 1) {
            throw new ApiException(ErrorResponseCode.INVALID_STATUS_TRANSITION);
        }
    }

    // ROUTE_ARRIVED로 갈 때만 호출됨
    private void validateCompleteRouteInput(UpdateRouteStatusRequest request) {
        if (request.actualDistance() == null || request.actualDuration() == null) {
            throw new ApiException(
                    ErrorResponseCode.INVALID_REQUEST,
                    "ROUTE_ARRIVED로 전이하려면 actualDistance/actualDuration이 필요합니다."
            );
        }

        // null 체크만으로는 -1 같은 음수 실측값도 그대로 저장돼버려서(운행 기록 정합성 깨짐),
        // 도착 처리 시점엔 두 값 다 0 이상인지도 같이 검증한다.
        if (request.actualDistance() < 0 || request.actualDuration() < 0) {
            throw new ApiException(
                    ErrorResponseCode.INVALID_REQUEST,
                    "actualDistance/actualDuration은 0 이상이어야 합니다."
            );
        }
    }

    /**
     * 배송을 논리 삭제한다. 물리 삭제가 아니라 deletedAt/deletedBy만 채운다(BaseUpdatableEntity.softDelete).
     * 딸린 구간(route)들도 같은 트랜잭션 안에서 전부 논리 삭제 처리한다 - 배송이 지워졌는데
     * 구간만 살아있으면 조회 결과가 뒤죽박죽되니까.
     * 권한 : 원본 스펙(배송 관리 - 삭제) 기준 MASTER/HUB_MANAGER(임시 무제한, TODO: 담당 허브 제한)만 가능,
     * DELIVERY_DRIVER/COMPANY_MANAGER는 불가.
     */
    @Override
    public void deleteDelivery(UUID deliveryId, UUID currentUserId, UserRole role) {
        // 이미 삭제된 배송을 또 삭제하려고 하면 자동으로 404
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new ApiException(ErrorResponseCode.DELIVERY_NOT_FOUND));

        // 권한 체크
        // 체담당자/배송담당자는 삭제 자체가 금지 -> 403
        validateDeleteAccess(role);

        // BaseUpdatableEntity에 이미 있던 메서드로 deletedAt/deletedBy만 채움
        delivery.softDelete(currentUserId);
        // 딸린 구간(route)들도 전부 softDelete()
        delivery.getRoutes().forEach(route -> route.softDelete(currentUserId));
    }

    private void validateDeleteAccess(UserRole role) {
        if (role == UserRole.DELIVERY_DRIVER || role == UserRole.COMPANY_MANAGER) {
            throw new ApiException(ErrorResponseCode.FORBIDDEN);
        }
    }

    /**
     * 배송을 취소(논리 삭제)한다 (internal 전용, Order 서비스가 주문 취소 시 호출).
     * DELIVERY_DRIVER/COMPANY_MANAGER 같은 역할 기반 권한 체크는 없다 - 로그인 사용자가 아니라
     * 서버 대 서버 호출이라 애초에 role 헤더 자체가 없기 때문. 대신 deliveryId가 정말
     * request.orderId() 소속인지만 검증한다.
     * 삭제 로직 자체는 deleteDelivery()와 동일(softDelete + routes cascade)하지만, 호출 주체와
     * 검증 내용이 달라서 별도 UseCase로 분리했다.
     */
    @Override
    public void cancelDelivery(UUID deliveryId, CancelDeliveryRequest request) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new ApiException(ErrorResponseCode.DELIVERY_NOT_FOUND));

        validateOrderOwnership(delivery, request.orderId());

        // 로그인 사용자가 아니라 시스템(Order 서비스)이 취소하는 것이므로 deletedBy는 null로 둔다.
        // JpaAuditingConfig의 AuditorAware도 X-User-Id 헤더가 없으면 createdBy/updatedBy를
        // null로 남기는 것과 동일한 방식 - 팀 컨벤션(시스템 처리 시 null)에 맞춤.
        delivery.softDelete(null);
        delivery.getRoutes().forEach(route -> route.softDelete(null));
    }

    // 다른 주문의 deliveryId로 잘못 취소 요청이 오는 걸 막는 안전장치
    // path의 deliveryId로 조회한 배송의 orderId(DB에 저장된 진짜 소속 주문)랑,
    // request body로 넘어온 orderId가 같은지 대조
    private void validateOrderOwnership(Delivery delivery, UUID orderId) {
        if (!delivery.getOrderId().equals(orderId)) {
            throw new ApiException(ErrorResponseCode.INVALID_REQUEST, "deliveryId에 해당하는 주문이 아닙니다.");
        }
    }

    /**
     * Hub 서비스에 최적 경로를 물어보면 구간별(허브-허브 사이) 거리/시간까지 한 번에 내려온다.
     * 그 segments를 그대로 DeliveryRoute로 매핑하기만 하면 되므로, 예전처럼 구간마다
     * Hub한테 따로 또 물어볼 필요가 없다.
     */
    private List<DeliveryRoute> buildRoutes(UUID departureHubId, UUID destinationHubId) {
        // Hub한테 출발허브에서 도착허브까지 최적경로(구간별 거리/시간 포함)를 물어봄.
        HubShortestRouteResponse shortestRoute =
                hubFeignClient.getShortestRoute(departureHubId, destinationHubId).data();

        // Hub 응답을 신뢰하지 않고 그대로 검증한다.
        // - data/segments가 null이거나 비어있으면 기존 코드는 조용히 빈 route 목록을
        //   만들어 저장까지 성공해버렸음(데이터 정합성 문제).
        // - Hub가 요청한 출발/도착 허브와 다른 경로를 내려주는 경우도 검증한다.
        validateShortestRoute(shortestRoute, departureHubId, destinationHubId);

        List<HubShortestRouteResponse.Segment> segments = shortestRoute.segments();

        return IntStream.range(0, segments.size())
                // IntStream(정수 스트림)을 Stream<DeliveryRoute>(객체 스트림)로 변환
                .mapToObj(i -> {
                    HubShortestRouteResponse.Segment segment = segments.get(i);
                    int sequence = i + 1; // 이해하기 쉽게 1부터 시작

                    // 이 구간을 담당할 기사를 정함
                    UUID segmentDriverId = assignHubDriver(segment.fromHubId());

                    return DeliveryRoute.create(
                            sequence,
                            segment.fromHubId(),
                            segment.toHubId(),
                            segment.distance(),
                            segment.duration(),
                            segmentDriverId
                    );
                })
                // 스트림을 최종적으로 List<DeliveryRoute>로 모음.
                .toList();
    }

    /**
     * Hub 서비스의 "최단 경로 조회" 응답이 실제로 신뢰할 수 있는 데이터인지 검증한다.
     * - data 자체가 없거나(요청 실패), 구간 목록(segments)이 없거나 비어있으면 즉시 예외로
     *   중단한다. (검증 없이는 route 0개인 Delivery가 조용히 저장됨)
     * - 구간에 hubId가 비어있거나, 첫 구간의 출발허브/마지막 구간의 도착허브가 요청한
     *   출발/도착 허브와 다르거나, 구간끼리 서로 안 이어지면(A→B, C→D처럼 끊기면)
     *   Hub가 엉뚱한 경로를 내려준 것이므로 마찬가지로 중단한다.
     */
    private void validateShortestRoute(
            HubShortestRouteResponse shortestRoute,
            UUID departureHubId,
            UUID destinationHubId
    ) {
        // 필드 자체가 비어있거나, segments 필드가 null이거나 비어있는 경우 예외처리
        // 참고 : 순서대로 작성해야 첫번째 조건에서 .segments()가 실행되지 않아 NPE가 나지않음
        if (shortestRoute == null
                || shortestRoute.segments() == null
                || shortestRoute.segments().isEmpty()) {
            throw new ApiException(ErrorResponseCode.HUB_ROUTE_NOT_FOUND);
        }

        List<HubShortestRouteResponse.Segment> segments = shortestRoute.segments();

        // fromHubId/toHubId가 null인 구간이 하나라도 있는지 검사.
        // 이걸 먼저 걸러야 아래 체인 검사(.toHubId().equals(...))에서 null에 대고
        // equals()를 호출하다 NPE가 나는 걸 막을 수 있다.
        boolean hasInvalidHubId = segments.stream()
                .anyMatch(segment -> segment.fromHubId() == null || segment.toHubId() == null);

        if (hasInvalidHubId) {
            throw new ApiException(ErrorResponseCode.HUB_ROUTE_NOT_FOUND);
        }

        // 구간끼리 순서대로 이어지는지 검사 (i번째 구간의 toHubId == i+1번째 구간의 fromHubId)
        boolean isChainBroken = IntStream.range(0, segments.size() - 1)
                .anyMatch(i -> !segments.get(i).toHubId().equals(segments.get(i + 1).fromHubId()));

        // 각각 "실제 출발 허브"와 "실제 도착 허브"
        UUID firstHubId = segments.get(0).fromHubId();
        UUID lastHubId = segments.get(segments.size() - 1).toHubId();

        // 구간 연결이 끊겨있거나, 우리가 물어본 출발 허브랑 Hub가 준 경로의 첫 허브가 다르거나,
        // 우리가 물어본 도착 허브랑 Hub가 준 경로의 끝 허브가 다른 경우 예외처리
        if (isChainBroken
                || !departureHubId.equals(firstHubId)
                || !destinationHubId.equals(lastHubId)) {
            throw new ApiException(ErrorResponseCode.HUB_ROUTE_NOT_FOUND);
        }
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
