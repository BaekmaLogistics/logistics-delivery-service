package com.sparta.logistics.application.query.usecase;

import com.sparta.logistics.application.query.dto.RouteResponse;
import com.sparta.logistics.common.constant.UserRole;

import java.util.List;
import java.util.UUID;

/**
 * 배송 구간(route) 목록 조회 UseCase.
 * 배송 상세 조회(DeliveryQueryUseCase)와 같은 접근 권한 규칙을 쓴다 - 특정 배송 하위 자원이라서.
 */
public interface RouteQueryUseCase {

    List<RouteResponse> getRoutes(UUID deliveryId, UUID currentUserId, UserRole role);
}
