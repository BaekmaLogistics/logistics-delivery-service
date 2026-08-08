package com.sparta.logistics.infrastructure.messaging.producer;

import com.sparta.logistics.domain.entity.Delivery;
import com.sparta.logistics.infrastructure.messaging.envelope.EventEnvelope;
import com.sparta.logistics.infrastructure.messaging.event.DeliveryStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 배송 관련 이벤트를 RabbitMQ로 발행하는 역할만 담당한다.
 * DeliveryCommandService가 AMQP 세부사항(exchange, routing key, RabbitTemplate)을
 * 직접 알 필요 없게 이 안으로 감춘다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DeliveryEventPublisher {

    private static final String EVENT_TYPE_STATUS_CHANGED = "DeliveryStatusChanged";

    private final RabbitTemplate rabbitTemplate;

    // QueueConfig가 큐를 바인딩할 때 쓴 것과 동일한 값(routing key = 큐 이름)이라
    // 같은 프로퍼티 키를 그대로 재사용한다.
    @Value("${message.exchange}")
    private String exchange;

    @Value("${message.queue.notification}")
    private String notificationRoutingKey;

    /**
     * 배송 상태가 바뀔 때 호출한다. Notification 서비스가 이 이벤트를 받아 슬랙 알림을 보낸다.
     * actorId는 이 상태변경을 요청한 사용자(currentUserId) - internal 호출 등으로 없을 수도 있어 nullable.
     */
    public void publishStatusChanged(Delivery delivery, UUID actorId) {
        // payload 만들기
        DeliveryStatusChangedEvent payload = DeliveryStatusChangedEvent.from(delivery);

        // EventEnvelope.of("DeliveryStatusChanged", payload, actorId)로 감싸기
        // -> header에 messageId, actorId, eventType, timestamp, version 자동으로 채워짐
        EventEnvelope<DeliveryStatusChangedEvent> envelope =
                EventEnvelope.of(EVENT_TYPE_STATUS_CHANGED, payload, actorId);

        // 메세지 발행
        rabbitTemplate.convertAndSend(exchange, notificationRoutingKey, envelope);

        log.info("Delivery status changed event published : deliveryId={}, status={}",
                delivery.getId(), delivery.getStatus());
    }
}
