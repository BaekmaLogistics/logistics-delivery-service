package com.sparta.logistics.infrastructure.messaging.outbox;

import com.sparta.logistics.infrastructure.persistence.jpa.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Outbox 패턴 구현체.
 * 비즈니스 트랜잭션(예: 배송 상태 변경)이 열려있는 트랜잭션 "안에서" 이 레코드를 같이 저장한다.
 * 그래서 "비즈니스 데이터 커밋"과 "이벤트가 나갈 예정이다"라는 사실이 원자적으로 묶인다 -
 * DB 커밋은 성공했는데 메시지 발행은 실패(또는 그 반대)하는 상황을 없앤다.
 * 실제 RabbitMQ 발행은 이 레코드를 만드는 시점에 하지 않고, OutboxRelay가 커밋된 PENDING
 * 레코드를 따로 폴링하면서 수행한다.
 */
@Entity
@Table(name = "p_outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA만 사용
public class OutboxEvent extends BaseUpdatableEntity {

    private static final int MAX_RETRY = 5;

    @Column(name = "exchange", nullable = false)
    private String exchange;

    @Column(name = "routing_key", nullable = false)
    private String routingKey;

    // 디버깅/조회 편의용 - 실제 발행 로직은 payload를 그대로 바이트로 보낼 뿐 이 값을 쓰지 않는다.
    @Column(name = "event_type", nullable = false)
    private String eventType;

    // EventEnvelope(header+payload)를 JSON 문자열로 직렬화해서 그대로 저장.
    // Relay가 이 문자열을 다시 객체로 역직렬화하지 않고 바이트로만 변환해서 그대로 발행한다.
    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "published_at")
    private Instant publishedAt;

    private OutboxEvent(String exchange, String routingKey, String eventType, String payload) {
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
    }

    public static OutboxEvent create(String exchange, String routingKey, String eventType, String payload) {
        return new OutboxEvent(exchange, routingKey, eventType, payload);
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    // 실패 시 재시도 횟수만 늘리고 PENDING을 유지 - 다음 폴링에서 다시 시도된다.
    // MAX_RETRY를 넘기면 FAILED로 전환해서 무한 재시도를 막는다(그 이후는 수동 확인 필요).
    public void markFailed() {
        this.retryCount++;
        if (this.retryCount >= MAX_RETRY) {
            this.status = OutboxStatus.FAILED;
        }
    }
}
