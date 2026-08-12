package com.sparta.logistics.domain.model;

/**
 * 배송 전체 상태.
 * HUB_WAITING(허브대기중) → HUB_MOVING(허브이동중) → HUB_ARRIVED(목적지허브도착)
 * → DELIVERING(배송중) → COMPANY_MOVING(업체이동중) → DELIVERED(배송완료)
 */
public enum DeliveryStatus {
    HUB_WAITING,
    HUB_MOVING,
    HUB_ARRIVED,
    DELIVERING,
    COMPANY_MOVING,
    DELIVERED
}
