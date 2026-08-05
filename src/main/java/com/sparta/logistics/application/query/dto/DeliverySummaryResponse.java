package com.sparta.logistics.application.query.dto;

import com.sparta.logistics.domain.entity.Delivery;
import com.sparta.logistics.domain.model.DeliveryStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * 배송 목록 조회(GET /deliveries)에서 한 건을 나타내는 요약 DTO.
 * 목록에서는 구간(routes) 상세까지는 내려주지 않는다 - 필요하면 상세 조회(GET /deliveries/{id})로 확인.
 */
public record DeliverySummaryResponse(
        UUID id,
        UUID orderId,
        DeliveryStatus status,
        UUID departureHubId,
        UUID destinationHubId,
        String receiverName,
        UUID companyDriverId,
        Instant createdAt
) {
    public static DeliverySummaryResponse from(Delivery delivery) {
        return new DeliverySummaryResponse(
                delivery.getId(),
                delivery.getOrderId(),
                delivery.getStatus(),
                delivery.getDepartureHubId(),
                delivery.getDestinationHubId(),
                delivery.getReceiverName(),
                delivery.getCompanyDriverId(),
                delivery.getCreatedAt()
        );
    }
}
