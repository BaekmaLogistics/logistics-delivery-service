package com.sparta.logistics.infrastructure.messaging.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.logistics.domain.entity.Delivery;
import com.sparta.logistics.infrastructure.messaging.envelope.EventEnvelope;
import com.sparta.logistics.infrastructure.messaging.event.DeliveryStatusChangedEvent;
import com.sparta.logistics.infrastructure.messaging.outbox.OutboxEvent;
import com.sparta.logistics.infrastructure.messaging.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 배송 관련 이벤트를 발행 "예약"하는 역할만 담당한다.
 * 실제 RabbitMQ 발행은 여기서 바로 하지 않고, outbox 테이블(OutboxEvent)에 PENDING 레코드를
 * 저장하는 것으로 끝낸다 - 이 저장은 호출한 쪽(DeliveryCommandService.updateStatus())이 이미
 * 열어놓은 트랜잭션 안에서 같이 커밋되므로, 배송 상태 변경과 "이벤트가 나갈 예정이다"라는
 * 사실이 원자적으로 묶인다. 실제 발행은 OutboxRelay가 커밋된 PENDING 레코드를 폴링하며 수행한다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DeliveryEventPublisher {

    private static final String EVENT_TYPE_STATUS_CHANGED = "DeliveryStatusChanged";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    // QueueConfig가 notification.queue를 바인딩할 때 쓴 것과 동일한 routing key.
    // (기존엔 큐 이름을 그대로 routing key로 썼는데, notification.queue가 여러 도메인
    // 이벤트를 받게 되면서 이벤트 타입별 routing key로 바뀌었다.)
    @Value("${message.exchange}")
    private String exchange;

    @Value("${message.binding-key.notification.delivery-status-changed}")
    private String notificationRoutingKey;

    /**
     * 배송 상태가 바뀔 때 호출한다. 실제 RabbitMQ 발행은 하지 않고 outbox 레코드만 저장한다.
     * actorId는 이 상태변경을 요청한 사용자(currentUserId) - internal 호출 등으로 없을 수도 있어 nullable.
     */
    public void publishStatusChanged(Delivery delivery, UUID actorId) {
        DeliveryStatusChangedEvent payload = DeliveryStatusChangedEvent.from(delivery);
        EventEnvelope<DeliveryStatusChangedEvent> envelope =
                EventEnvelope.of(EVENT_TYPE_STATUS_CHANGED, payload, actorId);

        String json = serialize(envelope);

        OutboxEvent outboxEvent =
                OutboxEvent.create(exchange, notificationRoutingKey, EVENT_TYPE_STATUS_CHANGED, json);
        outboxEventRepository.save(outboxEvent);

        log.info("Delivery status changed event queued (outbox) : deliveryId={}, status={}",
                delivery.getId(), delivery.getStatus());
    }

    private String serialize(EventEnvelope<DeliveryStatusChangedEvent> envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            // 직렬화 자체가 실패하는 건 outbox에 남겨도 영원히 발행 못 할 데이터라는 뜻이라,
            // 트랜잭션을 롤백시켜서 배송 상태변경 자체도 같이 실패하게 한다(이벤트 유실 방지).
            throw new IllegalStateException("이벤트 직렬화 실패", e);
        }
    }
}
