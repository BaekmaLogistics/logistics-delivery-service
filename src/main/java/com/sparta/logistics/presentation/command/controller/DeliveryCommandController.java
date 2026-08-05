package com.sparta.logistics.presentation.command.controller;

import com.sparta.logistics.application.command.dto.CreateDeliveryRequest;
import com.sparta.logistics.application.command.dto.DeliveryResponse;
import com.sparta.logistics.application.command.usecase.CreateDeliveryUseCase;
import com.sparta.logistics.common.code.GeneralResponseCode;
import com.sparta.logistics.presentation.common.dto.response.GeneralResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배송 생성(내부용) API 컨트롤러.
 * Order 서비스가 주문 확정 시 호출하는 내부 전용 엔드포인트.
 */
@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryCommandController {

    private final CreateDeliveryUseCase createDeliveryUseCase;

    @PostMapping("/internal")
    public ResponseEntity<GeneralResponse<DeliveryResponse>> createDelivery(
            @Valid @RequestBody CreateDeliveryRequest request
    ) {
        DeliveryResponse response = createDeliveryUseCase.createDelivery(request);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.CREATED, response);
    }
}
