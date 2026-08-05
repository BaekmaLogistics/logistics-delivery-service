package com.sparta.logistics.application.command.usecase;

import com.sparta.logistics.application.command.dto.CreateDeliveryRequest;
import com.sparta.logistics.application.command.dto.DeliveryResponse;

/**
 * 배송 생성 UseCase.
 * Order 서비스로부터 배송 요청을 받아 Delivery + DeliveryRoute(전체 구간)를 한 번에 생성한다.
 */
public interface CreateDeliveryUseCase {

    DeliveryResponse createDelivery(CreateDeliveryRequest request);
}
