package com.sparta.logistics.application.command.usecase;

import com.sparta.logistics.application.command.dto.UpdateRouteStatusRequest;
import com.sparta.logistics.application.query.dto.RouteResponse;
import com.sparta.logistics.common.constant.UserRole;

import java.util.UUID;

/**
 * 배송 구간(route) 상태 전이 UseCase.
 * ROUTE_WAITING → ROUTE_MOVING → ROUTE_ARRIVED → DELIVERING 순서를 하나씩만 전진할 수 있다.
 * ROUTE_ARRIVED로 전이할 때는 실제 거리/소요시간을 같이 기록한다(DeliveryRoute.completeRoute).
 */
public interface UpdateRouteStatusUseCase {

    RouteResponse updateRouteStatus(
            UUID deliveryId,
            UUID routeId,
            UpdateRouteStatusRequest request,
            UUID currentUserId,
            UserRole role
    );
}
