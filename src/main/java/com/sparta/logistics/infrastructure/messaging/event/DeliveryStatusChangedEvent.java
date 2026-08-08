package com.sparta.logistics.infrastructure.messaging.event;

import com.sparta.logistics.domain.entity.Delivery;
import com.sparta.logistics.domain.model.DeliveryStatus;

import java.util.UUID;

/**
 * 배송 상태 변경 시 notification.queue로 발행하는 이벤트의 payload.
 * Notification 서비스가 이 정보로 슬랙 알림 메시지를 만들어 보낸다.
 * 필드는 최소한으로 시작 - Notification 담당자와 스펙 확정되면 조정 예정.
 */
public record DeliveryStatusChangedEvent(
        UUID deliveryId,
        UUID orderId,
        DeliveryStatus status
) {
    public static DeliveryStatusChangedEvent from(Delivery delivery) {
        return new DeliveryStatusChangedEvent(
                delivery.getId(),
                delivery.getOrderId(),
                delivery.getStatus()
        );
    }
}
