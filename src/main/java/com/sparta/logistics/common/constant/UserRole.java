package com.sparta.logistics.common.constant;

/**
 * Gateway가 X-User-Role 헤더로 내려주는 사용자 역할.
 * ErrorResponseCode/ApiException처럼 presentation 계층뿐 아니라 domain(repository)/application(service)
 * 계층에서도 같이 참조하는 값이라 common 쪽에 둔다.
 * 원 요구사항(권한 표) 기준 4개 역할 - "배송 담당자"는 Delivery.companyDriverId(업체배송담당자)로
 * 배정됐든 DeliveryRoute.driverId(구간 배송담당자)로 배정됐든 구분하지 않고 하나로 취급한다.
 * - MASTER : 전체 배송 조회 가능(제한 없음)
 * - HUB_MANAGER : 원래는 "담당 허브"로 제한돼야 하지만, 담당 허브 정보를 지금 조회할 방법이 없어서
 *   1차 구현에서는 MASTER와 동일하게 전체 조회 허용 (TODO: User&Auth 쪽 확인되면 제한 로직 추가)
 * - DELIVERY_DRIVER : 자신이 담당하는 배송만 조회 가능
 *   (Delivery.companyDriverId가 본인이거나, 소속 route 중 하나라도 driverId가 본인인 경우)
 * - COMPANY_MANAGER : 원래는 "본인 주문건"으로 제한돼야 하지만, Delivery는 orderId만 갖고 있고
 *   "이 주문이 내 소속인지"를 판단할 정보가 없어서(Order 서비스 쪽 확인 필요)
 *   1차 구현에서는 MASTER와 동일하게 전체 조회 허용 (TODO: Order 서비스 쪽 확인되면 제한 로직 추가)
 */
public enum UserRole {
    MASTER,
    HUB_MANAGER,
    DELIVERY_DRIVER,
    COMPANY_MANAGER
}
