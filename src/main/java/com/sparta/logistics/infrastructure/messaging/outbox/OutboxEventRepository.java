package com.sparta.logistics.infrastructure.messaging.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * OutboxRelay가 폴링할 때 쓴다. 오래된 것부터 처리(생성 순서 보장), pageable로 배치 크기 제한.
     */
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);
}
