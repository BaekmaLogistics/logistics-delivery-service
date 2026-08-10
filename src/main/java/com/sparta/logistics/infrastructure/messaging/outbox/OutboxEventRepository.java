package com.sparta.logistics.infrastructure.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * OutboxRelay가 폴링할 때 쓴다. PENDING 레코드를 오래된 것부터 최대 limit개 가져오면서
     * 동시에 그 행들을 잠근다(FOR UPDATE SKIP LOCKED).
     * 서비스 인스턴스가 여러 개 떠 있어도(수평 확장), 이미 다른 인스턴스가 잠근 행은
     * 건너뛰고 아직 안 잠긴 것만 가져가기 때문에, 같은 이벤트를 두 인스턴스가 동시에 집어서
     * 중복 발행하는 문제가 없다. 이 메서드를 호출하는 트랜잭션이 끝나면(커밋/롤백) 잠금은
     * 자동으로 풀린다 - 인스턴스가 처리 도중 죽어도 잠금이 영원히 안 풀리는 일은 없다.
     */
    @Query(
            value = "SELECT * FROM p_outbox_events "
                    + "WHERE status = 'PENDING' "
                    + "ORDER BY created_at ASC "
                    + "LIMIT :limit "
                    + "FOR UPDATE SKIP LOCKED",
            nativeQuery = true
    )

    List<OutboxEvent> findPendingForUpdateSkipLocked(@Param("limit") int limit);
}
