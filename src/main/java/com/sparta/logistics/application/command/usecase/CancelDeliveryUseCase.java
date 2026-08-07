package com.sparta.logistics.application.command.usecase;

import com.sparta.logistics.application.command.dto.CancelDeliveryRequest;

import java.util.UUID;

/**
 * 배송 취소 UseCase.
 * Order 서비스가 주문을 취소했을 때, 연관된 배송을 논리 삭제 처리한다.
 * (배송 관리 스펙: "주문이 취소되거나 삭제될 때 연관된 데이터도 삭제 관련 필드를 통해 관리")
 */
public interface CancelDeliveryUseCase {

    void cancelDelivery(UUID deliveryId, CancelDeliveryRequest request);
}
