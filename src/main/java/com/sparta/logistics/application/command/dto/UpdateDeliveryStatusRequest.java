package com.sparta.logistics.application.command.dto;

import com.sparta.logistics.domain.model.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 배송 상태 변경 요청 DTO.
 * status는 현재 상태의 "바로 다음" 상태만 허용된다 (건너뛰기/역행 불가).
 */
public record UpdateDeliveryStatusRequest(
        @NotNull DeliveryStatus status
) {
}
