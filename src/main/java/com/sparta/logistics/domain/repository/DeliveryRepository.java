package com.sparta.logistics.domain.repository;

import com.sparta.logistics.domain.entity.Delivery;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID>, DeliveryQueryRepository {

    /**
     * 상세 조회용. soft-delete된 배송은 제외하고, routes(구간)까지 한 번에 fetch join으로 가져온다.
     * (EntityGraph 없이 delivery.getRoutes()를 호출하면 지연로딩 때문에 쿼리가 한 번 더 나감)
     */
    // 쿼리 실행할 때 routes도 같이 가져옴
    @EntityGraph(attributePaths = "routes")
    Optional<Delivery> findByIdAndDeletedAtIsNull(UUID id); // id가 같고 deletedAt이 null인 것
}
