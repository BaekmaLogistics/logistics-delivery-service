package com.sparta.logistics.common.constant;

/**
 * Gateway가 X-User-Role 헤더로 내려주는 사용자 역할.
 * ErrorResponseCode/ApiException처럼 presentation 계층뿐 아니라 domain(repository)/application(service)
 * 계층에서도 같이 참조하는 값이라 common 쪽에 둔다.
 * 원 요구사항(권한 표) 기준 4개 역할 - "배송 담당자"는 Delivery.companyDriverId(업체배송담당자)로
 * 배정됐든 DeliveryRoute.driverId(구간 배송담당자)로 배정됐든 구분하지 않고 하나로 취급한다.
 * - MASTER : 전체 배송 조회/수정/삭제 가능(제한 없음)
 * - HUB_MANAGER : 담당 허브(출발/도착 허브 둘 중 하나)가 걸린 배송만 조회/수정/삭제 가능.
 *   담당 허브 ID는 User&Auth 서비스 단건 조회(UserFeignClient.getUserInfo)로 매 요청마다 확인한다.
 * - DELIVERY_DRIVER : 자신이 담당하는 배송만 조회 가능
 *   (Delivery.companyDriverId가 본인이거나, 소속 route 중 하나라도 driverId가 본인인 경우)
 * - COMPANY_MANAGER : 소속 업체(Delivery.companyId와 일치)의 배송만 조회 가능, 수정/삭제는 불가.
 *   Order 서비스가 배송 생성 요청 시 companyId를 넘겨줘서 Delivery에 저장해두고 그걸로 대조한다.
 */
public enum UserRole {
    MASTER,
    HUB_MANAGER,
    DELIVERY_DRIVER,
    COMPANY_MANAGER
}
