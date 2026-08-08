package com.sparta.logistics.infrastructure.messaging.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * PENDING 상태인 outbox 레코드를 주기적으로 폴링해서 실제로 RabbitMQ에 발행하는 역할.
 * 발행에 성공한 레코드만 PUBLISHED로 표시하고, 실패하면 재시도 횟수만 늘려두고 다음
 * 폴링에서 다시 시도한다(OutboxEvent.markFailed()가 최대 재시도 초과 시 FAILED로 전환).
 * 이 클래스가 도는 시점엔 원래 배송 상태변경 트랜잭션은 이미 커밋된 뒤이므로, 여기서 실패해도
 * 배송 데이터에는 영향이 없다 - 이벤트만 재시도 대상으로 남는다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxRelay {

    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    // 메서드를 주기적으로 실행
    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay:3000}")
    @Transactional
    // PENDING 레코드를 최대 50개 가져와서 하나씩 publish() 호출
    public void relay() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, BATCH_SIZE)
        );

        pendingEvents.forEach(this::publish);
        // 트랜잭션 안에서 영속 상태 엔티티들이라 markPublished()/markFailed()로 바뀐 상태가
        // 더티체킹으로 커밋 시 반영된다 - 별도 save() 호출 불필요.
    }

    private void publish(OutboxEvent event) {
        try {
            rabbitTemplate.send(event.getExchange(), event.getRoutingKey(), toMessage(event));
            event.markPublished();
        } catch (Exception e) {
            event.markFailed();
            log.error("Outbox event publish failed : id={}, eventType={}, retryCount={}",
                    event.getId(), event.getEventType(), event.getRetryCount(), e);
        }
    }

    /**
     * payload에 이미 JSON 문자열이 저장돼 있으므로, 컨버터를 다시 거치지 않고
     * 바이트로만 변환해서 그대로 발행한다(직렬화를 두 번 하지 않기 위함).
     */
    private Message toMessage(OutboxEvent event) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setContentEncoding("UTF-8");

        return new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), properties);
    }
}
