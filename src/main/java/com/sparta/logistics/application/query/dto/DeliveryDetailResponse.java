package com.sparta.logistics.application.query.dto;

import com.sparta.logistics.domain.entity.Delivery;
import com.sparta.logistics.domain.entity.DeliveryRoute;
import com.sparta.logistics.domain.model.DeliveryStatus;
import com.sparta.logistics.domain.model.RouteStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 배송 상세 조회(GET /deliveries/{deliveryId}) 응답 DTO.
 * 구간(routes)까지 전부 포함해서 내려준다.
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
        List<RouteDetail> routes
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
                        .map(RouteDetail::from)
                        .toList()
        );
    }

    public record RouteDetail(
            UUID id,
            Integer sequence,
            UUID fromHubId,
            UUID toHubId,
            Double expectedDistance,
            Integer expectedDuration,
            Double actualDistance,
            Integer actualDuration,
            RouteStatus status,
            UUID driverId
    ) {
        public static RouteDetail from(DeliveryRoute route) {
            return new RouteDetail(
                    route.getId(),
                    route.getSequence(),
                    route.getFromHubId(),
                    route.getToHubId(),
                    route.getExpectedDistance(),
                    route.getExpectedDuration(),
                    route.getActualDistance(),
                    route.getActualDuration(),
                    route.getStatus(),
                    route.getDriverId()
            );
        }
    }
}
