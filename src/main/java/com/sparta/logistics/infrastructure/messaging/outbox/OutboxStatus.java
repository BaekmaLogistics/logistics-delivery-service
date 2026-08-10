package com.sparta.logistics.infrastructure.messaging.outbox;

/**
 * Outbox 레코드의 발행 상태.
 * - PENDING : 아직 발행 안 됨 (OutboxRelay가 다음 폴링에서 시도 대상)
 * - PUBLISHED : 발행 성공
 * - FAILED : 재시도 최대 횟수를 넘겨서 더 이상 자동 재시도하지 않음
 */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
