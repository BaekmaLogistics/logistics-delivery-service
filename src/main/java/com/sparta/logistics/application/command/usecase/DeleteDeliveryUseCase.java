package com.sparta.logistics.application.command.usecase;

import com.sparta.logistics.common.constant.UserRole;

import java.util.UUID;

/**
 * 배송 논리 삭제(soft delete) UseCase.
 * 배송이 삭제되면 딸린 구간(DeliveryRoute)도 같이 논리 삭제된다.
 */
public interface DeleteDeliveryUseCase {

    void deleteDelivery(UUID deliveryId, UUID currentUserId, UserRole role);
}
