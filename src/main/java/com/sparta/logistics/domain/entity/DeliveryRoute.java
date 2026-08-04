package com.sparta.logistics.domain.entity;

import com.sparta.logistics.domain.model.RouteStatus;
import com.sparta.logistics.infrastructure.persistence.jpa.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 배송 경로상 구간별 기록.
 * 하나의 배송(Delivery)에 여러 구간(sequence 순)이 딸린다 - 배송 생성 시 전체 구간이 한 번에 생성됨.
 */
@Entity
@Table(
        name = "p_delivery_route",
        uniqueConstraints = @UniqueConstraint(columnNames = {"delivery_id", "sequence"})
        // 같은 배송 안에서 같은 순번(sequence)이 두 번 나오면 안 됨.
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA용 빈 생성자를 protected로 막아둠.
public class DeliveryRoute extends BaseUpdatableEntity {

    // 여러 개의 DeliveryRoute가 하나의 Delivery를 가리킨다"는 뜻
    // N:1 관계의 N쪽.
    // DeliveryRoute를 DB에서 조회할 때, delivery 필드는 즉시 같이 안 가져오고 프록시(가짜 객체)만 넣어둠.
    // 실제로 route.getDelivery().getOrderId()처럼 이 필드를 "사용"하는 순간에야 진짜 쿼리가 나가서 Delivery를 가져옴.
    // FK 컬럼은 오직 @ManyToOne+@JoinColumn 쪽에만 생김.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @Column(name = "from_hub_id", nullable = false)
    private UUID fromHubId;

    @Column(name = "to_hub_id", nullable = false)
    private UUID toHubId;

    @Column(name = "expected_distance", nullable = false)
    private Double expectedDistance;

    @Column(name = "expected_duration", nullable = false)
    private Integer expectedDuration;

    /** 구간 완료 전까지 NULL */
    @Column(name = "actual_distance")
    private Double actualDistance;

    /** 구간 완료 전까지 NULL */
    @Column(name = "actual_duration")
    private Integer actualDuration;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RouteStatus status;

    // 이 구간(허브-허브 사이)을 담당하는 기사
    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    // 어느 배송에 속하는지는 생성 시점에 안 정해주고,
    // 나중에 Delivery.addRoute() → route.assignTo(this)를 통해서 따로 연결
    private DeliveryRoute(
            Integer sequence,
            UUID fromHubId,
            UUID toHubId,
            Double expectedDistance,
            Integer expectedDuration,
            UUID driverId
    ) {
        this.sequence = sequence;
        this.fromHubId = fromHubId;
        this.toHubId = toHubId;
        this.expectedDistance = expectedDistance;
        this.expectedDuration = expectedDuration;
        this.driverId = driverId;
        this.status = RouteStatus.ROUTE_WAITING;
    }

    public static DeliveryRoute create(
            Integer sequence,
            UUID fromHubId,
            UUID toHubId,
            Double expectedDistance,
            Integer expectedDuration,
            UUID driverId
    ) {
        return new DeliveryRoute(sequence, fromHubId, toHubId, expectedDistance, expectedDuration, driverId);
    }

    // 부모를 넘겨받아서 저장해둠.
    // 나(route)는 이제부터 이 delivery를 내 부모로 기억한다.는 뜻
    /** Delivery.addRoute()에서만 호출 - 연관관계 편의 메서드 */
    void assignTo(Delivery delivery) {
        this.delivery = delivery;
    }

    public void changeStatus(RouteStatus newStatus) {
        this.status = newStatus;
    }

    /** 구간 도착 시 실제 거리/소요시간을 기록한다. */
    public void completeRoute(Double actualDistance, Integer actualDuration) {
        this.actualDistance = actualDistance;
        this.actualDuration = actualDuration;
        this.status = RouteStatus.ROUTE_ARRIVED;
    }
}
