package com.sparta.logistics.application.query.dto;

import com.sparta.logistics.domain.entity.Delivery;
import com.sparta.logistics.domain.model.DeliveryStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 배송 상세 조회(GET /deliveries/{deliveryId}) 응답 DTO.
 * 구간(routes)까지 전부 포함해서 내려준다.
 * 구간 하나하나의 모양은 RouteResponse(구간 목록조회/상태수정 API와 공통) 참고.
 */
public record DeliveryDetailResponse(
        UUID id,
        UUID orderId,
        DeliveryStatus status,
        UUID departureHubId,
        UUID destinationHubId,
        String deliveryAddress,
        String receiverName,
        String receiverSlackId,
        UUID companyDriverId,
        Instant createdAt,
        Instant updatedAt,
        List<RouteResponse> routes
) {
    public static DeliveryDetailResponse from(Delivery delivery) {
        return new DeliveryDetailResponse(
                delivery.getId(),
                delivery.getOrderId(),
                delivery.getStatus(),
                delivery.getDepartureHubId(),
                delivery.getDestinationHubId(),
                delivery.getDeliveryAddress(),
                delivery.getReceiverName(),
                delivery.getReceiverSlackId(),
                delivery.getCompanyDriverId(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt(),
                delivery.getRoutes().stream()
                        .map(RouteResponse::from)
                        .toList()
        );
    }
}
