package com.sparta.logistics.application.command.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 배송 생성 요청 DTO.
 * Order 서비스가 주문 확정 시 내부 API(POST /api/v1/deliveries/internal)로 호출한다.
 * 배송경로기록(구간)은 Order가 넘겨주지 않고, Delivery 서비스가 Hub 서비스의
 * "최적 경로 조회" + "허브 연결 목록 조회" Feign 호출로 직접 계산해서 생성한다.
 */
public record CreateDeliveryRequest(
        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        @NotNull(message = "출발 허브 ID는 필수입니다.")
        UUID departureHubId,

        @NotNull(message = "도착 허브 ID는 필수입니다.")
        UUID destinationHubId,

        @NotBlank(message = "배송지 주소는 필수입니다.")
        String deliveryAddress,

        @NotBlank(message = "수령인 이름은 필수입니다.")
        String receiverName,

        @NotBlank(message = "수령인 슬랙 ID는 필수입니다.")
        String receiverSlackId
) {
}
