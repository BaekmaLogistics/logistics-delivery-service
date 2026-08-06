package com.sparta.logistics.presentation.command.controller;

import com.sparta.logistics.application.command.dto.CreateDeliveryRequest;
import com.sparta.logistics.application.command.dto.DeliveryResponse;
import com.sparta.logistics.application.command.dto.UpdateDeliveryStatusRequest;
import com.sparta.logistics.application.command.usecase.CreateDeliveryUseCase;
import com.sparta.logistics.application.command.usecase.UpdateDeliveryStatusUseCase;
import com.sparta.logistics.common.code.GeneralResponseCode;
import com.sparta.logistics.common.constant.UserRole;
import com.sparta.logistics.presentation.common.constant.HeaderConstants;
import com.sparta.logistics.presentation.common.dto.response.GeneralResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 배송 생성(내부용) / 상태 변경 API 컨트롤러.
 * 생성은 Order 서비스가 주문 확정 시 호출하는 내부 전용 엔드포인트.
 * 상태 변경은 Gateway가 인증을 마친 뒤 X-User-Id / X-User-Role 헤더로 요청자 정보를 내려준다는 전제.
 */
@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryCommandController {

    private final CreateDeliveryUseCase createDeliveryUseCase;
    private final UpdateDeliveryStatusUseCase updateDeliveryStatusUseCase;

    @PostMapping("/internal")
    public ResponseEntity<GeneralResponse<DeliveryResponse>> createDelivery(
            @Valid @RequestBody CreateDeliveryRequest request
    ) {
        DeliveryResponse response = createDeliveryUseCase.createDelivery(request);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.CREATED, response);
    }

    @PatchMapping("/{deliveryId}/status")
    public ResponseEntity<GeneralResponse<DeliveryResponse>> updateDeliveryStatus(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody UpdateDeliveryStatusRequest request,
            @RequestHeader(HeaderConstants.USER_ID) UUID currentUserId,
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole role
    ) {
        DeliveryResponse response =
                updateDeliveryStatusUseCase.updateStatus(deliveryId, request, currentUserId, role);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }
}
