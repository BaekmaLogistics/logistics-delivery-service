package com.sparta.logistics.domain.entity;

import com.sparta.logistics.domain.model.DeliveryStatus;
import com.sparta.logistics.infrastructure.persistence.jpa.entity.BaseUpdatableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 배송 전체 상태를 관리하는 엔티티(Aggregate Root).
 * 배송경로기록(DeliveryRoute)을 자식으로 가진다 - 배송 생성 시 전체 구간이 한 번에 생성됨.
 */
@Entity
@Table(name = "p_deliveries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA만 사용
public class Delivery extends BaseUpdatableEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING) // enum을 문자열로 저장
    @Column(name = "status", nullable = false)
    private DeliveryStatus status;

    @Column(name = "departure_hub_id", nullable = false)
    private UUID departureHubId;

    @Column(name = "destination_hub_id", nullable = false)
    private UUID destinationHubId;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Column(name = "receiver_slack_id", nullable = false)
    private String receiverSlackId;

    // 전체 배송 담당 기사
    @Column(name = "company_driver_id", nullable = false)
    private UUID companyDriverId;

    // 1(Delivery) : N(DeliveryRoute)
    // DeliveryRoute 클래스 안에 있는 delivery라는 필드 이름
    // Delivery 엔티티에 대해 저장(persist)/삭제(remove) 등의 작업이 일어나면, routes에 들어있는 DeliveryRoute들한테도 똑같은 작업이 전파
    // routes 리스트에서 특정 route 객체를 빼버리면(routes.remove(...)), 그 route는 DB에서도 자동으로 DELETE
    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<DeliveryRoute> routes = new ArrayList<>();

    // private 생성자
    // 배송은 무조건 HUB_WAITING 상태로 시작
    private Delivery(
            UUID orderId,
            UUID departureHubId,
            UUID destinationHubId,
            String deliveryAddress,
            String receiverName,
            String receiverSlackId,
            UUID companyDriverId
    ) {
        this.orderId = orderId;
        this.status = DeliveryStatus.HUB_WAITING;
        this.departureHubId = departureHubId;
        this.destinationHubId = destinationHubId;
        this.deliveryAddress = deliveryAddress;
        this.receiverName = receiverName;
        this.receiverSlackId = receiverSlackId;
        this.companyDriverId = companyDriverId;
    }

    // create() 안에서 생성자 호출 : status = HUB_WAITING으로 자동 초기화
    public static Delivery create(
            UUID orderId,
            UUID departureHubId,
            UUID destinationHubId,
            String deliveryAddress,
            String receiverName,
            String receiverSlackId,
            UUID companyDriverId
    ) {
        return new Delivery(
                orderId,
                departureHubId,
                destinationHubId,
                deliveryAddress,
                receiverName,
                receiverSlackId,
                companyDriverId
        );
    }

    /** 구간(route)을 이 배송에 소속시킨다. 배송 생성 시 전체 구간을 한 번에 추가. */
    public void addRoute(DeliveryRoute route) {
        route.assignTo(this); // DeliveryRoute 쪽에도 "네 부모(delivery)는 나야"라고 알려줌.
        this.routes.add(route); // Delivery 쪽 리스트에도 추가.
    }

    public void changeStatus(DeliveryStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * 이 배송에 대해 userId가 "담당자"인지 확인한다.
     * 전체 담당(companyDriverId)이거나, 소속 구간(route) 중 하나라도 담당(driverId)이면 true.
     * DELIVERY_DRIVER 권한 체크에서 조회/상태변경 양쪽에서 공통으로 쓰인다.
     */
    public boolean isAssignedTo(UUID userId) {
        return companyDriverId.equals(userId)
                || routes.stream().anyMatch(route -> route.getDriverId().equals(userId));
    }
}
