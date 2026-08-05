package com.sparta.logistics.presentation.query.controller;

import com.sparta.logistics.application.query.dto.DeliveryDetailResponse;
import com.sparta.logistics.application.query.dto.DeliveryPageResponse;
import com.sparta.logistics.application.query.usecase.DeliveryQueryUseCase;
import com.sparta.logistics.common.code.GeneralResponseCode;
import com.sparta.logistics.common.constant.UserRole;
import com.sparta.logistics.domain.model.DeliveryStatus;
import com.sparta.logistics.presentation.common.constant.HeaderConstants;
import com.sparta.logistics.presentation.common.dto.response.GeneralResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 배송 조회(상세 + 목록 검색) API 컨트롤러.
 * Gateway가 인증을 마친 뒤 X-User-Id / X-User-Role 헤더로 요청자 정보를 내려준다는 전제.
 */
@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryQueryController {

    private final DeliveryQueryUseCase deliveryQueryUseCase;

    @GetMapping("/{deliveryId}")
    public ResponseEntity<GeneralResponse<DeliveryDetailResponse>> getDeliveryDetail(
            @PathVariable UUID deliveryId,
            @RequestHeader(HeaderConstants.USER_ID) UUID currentUserId,
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole role
    ) {
        DeliveryDetailResponse response =
                deliveryQueryUseCase.getDeliveryDetail(deliveryId, currentUserId, role);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    @GetMapping
    public ResponseEntity<GeneralResponse<DeliveryPageResponse>> getDeliveries(
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(required = false) UUID hubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(HeaderConstants.USER_ID) UUID currentUserId,
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole role
    ) {
        DeliveryPageResponse response =
                deliveryQueryUseCase.getDeliveries(status, hubId, page, size, currentUserId, role);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }
}
