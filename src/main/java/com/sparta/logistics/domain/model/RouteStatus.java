package com.sparta.logistics.domain.model;

/**
 * 배송경로기록(구간) 상태.
 * ROUTE_WAITING(허브이동대기중) → ROUTE_MOVING(허브이동중) → ROUTE_ARRIVED(목적지허브도착) → DELIVERING(배송중)
 */
public enum RouteStatus {
    ROUTE_WAITING,
    ROUTE_MOVING,
    ROUTE_ARRIVED,
    DELIVERING
}
