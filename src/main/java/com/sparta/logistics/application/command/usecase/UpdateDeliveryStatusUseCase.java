package com.sparta.logistics.application.command.usecase;

import com.sparta.logistics.application.command.dto.DeliveryResponse;
import com.sparta.logistics.application.command.dto.UpdateDeliveryStatusRequest;
import com.sparta.logistics.common.constant.UserRole;

import java.util.UUID;

/**
 * 배송 상태 전이 UseCase.
 * HUB_WAITING → HUB_MOVING → HUB_ARRIVED → DELIVERING → COMPANY_MOVING → DELIVERED
 * 순서를 하나씩만 전진할 수 있고, 건너뛰거나 역행하면 INVALID_STATUS_TRANSITION.
 */
public interface UpdateDeliveryStatusUseCase {

    DeliveryResponse updateStatus(
            UUID deliveryId,
            UpdateDeliveryStatusRequest request,
            UUID currentUserId,
            UserRole role
    );
}
