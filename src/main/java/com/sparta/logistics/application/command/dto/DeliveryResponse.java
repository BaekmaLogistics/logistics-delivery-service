package com.sparta.logistics.application.command.dto;

import com.sparta.logistics.domain.entity.Delivery;
import com.sparta.logistics.domain.entity.DeliveryRoute;
import com.sparta.logistics.domain.model.DeliveryStatus;
import com.sparta.logistics.domain.model.RouteStatus;

import java.util.List;
import java.util.UUID;

/**
 * 배송 생성 결과 응답 DTO.
 */
public record DeliveryResponse(
        UUID id,
        UUID orderId,
        DeliveryStatus status,
        UUID departureHubId,
        UUID destinationHubId,
        String deliveryAddress,
        String receiverName,
        String receiverSlackId,
        UUID companyDriverId,
        List<RouteSegmentResponse> routes
) {
    public static DeliveryResponse from(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getOrderId(),
                delivery.getStatus(),
                delivery.getDepartureHubId(),
                delivery.getDestinationHubId(),
                delivery.getDeliveryAddress(),
                delivery.getReceiverName(),
                delivery.getReceiverSlackId(),
                delivery.getCompanyDriverId(),
                delivery.getRoutes().stream()
                        .map(RouteSegmentResponse::from)
                        .toList()
        );
    }

    public record RouteSegmentResponse(
            UUID id,
            Integer sequence,
            UUID fromHubId,
            UUID toHubId,
            Double expectedDistance,
            Integer expectedDuration,
            RouteStatus status,
            UUID driverId
    ) {
        public static RouteSegmentResponse from(DeliveryRoute route) {
            return new RouteSegmentResponse(
                    route.getId(),
                    route.getSequence(),
                    route.getFromHubId(),
                    route.getToHubId(),
                    route.getExpectedDistance(),
                    route.getExpectedDuration(),
                    route.getStatus(),
                    route.getDriverId()
            );
        }
    }
}
