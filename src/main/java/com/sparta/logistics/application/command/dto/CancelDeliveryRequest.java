package com.sparta.logistics.application.command.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 배송 취소 요청 DTO.
 * Order 서비스가 주문 취소 시 내부 API(PATCH /internal/api/v1/deliveries/{deliveryId}/cancel)로 호출한다.
 */
public record CancelDeliveryRequest(
        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        String reason
) {
}
