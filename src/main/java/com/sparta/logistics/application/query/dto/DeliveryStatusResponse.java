package com.sparta.logistics.application.query.dto;

import com.sparta.logistics.domain.entity.Delivery;
import com.sparta.logistics.domain.model.DeliveryStatus;

import java.util.UUID;

/**
 * 배송 진행 상태 확인 응답 DTO (internal 전용).
 * Order 서비스가 배송 진행 상태만 가볍게 확인할 때 쓴다. 상세 정보(주소/수령인 등)는 필요 없어서
 * DeliveryDetailResponse를 그대로 쓰지 않고 최소 필드만 내려준다.
 */
public record DeliveryStatusResponse(
        UUID deliveryId,
        DeliveryStatus status
) {
    public static DeliveryStatusResponse from(Delivery delivery) {
        return new DeliveryStatusResponse(delivery.getId(), delivery.getStatus());
    }
}
