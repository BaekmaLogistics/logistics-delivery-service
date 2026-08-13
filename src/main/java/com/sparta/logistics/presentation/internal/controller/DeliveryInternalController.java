package com.sparta.logistics.presentation.internal.controller;

import com.sparta.logistics.application.command.dto.CancelDeliveryRequest;
import com.sparta.logistics.application.command.dto.CreateDeliveryRequest;
import com.sparta.logistics.application.command.dto.DeliveryResponse;
import com.sparta.logistics.application.command.usecase.CancelDeliveryUseCase;
import com.sparta.logistics.application.command.usecase.CreateDeliveryUseCase;
import com.sparta.logistics.application.query.dto.DeliveryDetailResponse;
import com.sparta.logistics.application.query.dto.DeliveryStatusResponse;
import com.sparta.logistics.application.query.dto.RouteResponse;
import com.sparta.logistics.application.query.usecase.DeliveryQueryUseCase;
import com.sparta.logistics.application.query.usecase.RouteQueryUseCase;
import com.sparta.logistics.common.code.GeneralResponseCode;
import com.sparta.logistics.presentation.common.dto.response.GeneralResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Order/Notification 서비스가 서버 대 서버로 호출하는 internal 전용 API 컨트롤러.
 * 팀 컨벤션에 따라 /internal/api/v1/ 프리픽스를 쓰고, Gateway가 /internal/** 요청을
 * 외부망에서 전부 403 처리하므로 여기서는 로그인 사용자 헤더(X-User-Id/X-User-Role)나
 * 역할 기반 권한 체크를 하지 않는다 (내부망 격리로 대체 - 팀 튜터님 확인 완료).
 */
@RestController
@RequestMapping("/internal/api/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryInternalController {

    private final CreateDeliveryUseCase createDeliveryUseCase;
    private final CancelDeliveryUseCase cancelDeliveryUseCase;
    private final DeliveryQueryUseCase deliveryQueryUseCase;
    private final RouteQueryUseCase routeQueryUseCase;

    @SecurityRequirements // internal API - JWT 헤더 불필요 (Gateway가 /internal/** 외부 접근 차단)
    @PostMapping
    public ResponseEntity<GeneralResponse<DeliveryResponse>> createDelivery(
            @Valid @RequestBody CreateDeliveryRequest request
    ) {
        DeliveryResponse response = createDeliveryUseCase.createDelivery(request);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.CREATED, response);
    }

    // 주문 취소에 따른 배송 취소
    @SecurityRequirements // internal API - JWT 헤더 불필요
    @PatchMapping("/{deliveryId}/cancel")
    public ResponseEntity<GeneralResponse<Void>> cancelDelivery(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody CancelDeliveryRequest request
    ) {
        cancelDeliveryUseCase.cancelDelivery(deliveryId, request);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, null);
    }

    // 배송 상태 확인
    @SecurityRequirements // internal API - JWT 헤더 불필요
    @GetMapping("/{deliveryId}/status")
    public ResponseEntity<GeneralResponse<DeliveryStatusResponse>> getDeliveryStatus(
            @PathVariable UUID deliveryId
    ) {
        DeliveryStatusResponse response = deliveryQueryUseCase.getDeliveryStatus(deliveryId);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    // 배송 조회 (Notification이 슬랙 메시지 생성 시 호출)
    @SecurityRequirements // internal API - JWT 헤더 불필요
    @GetMapping("/{deliveryId}")
    public ResponseEntity<GeneralResponse<DeliveryDetailResponse>> getDeliveryDetail(
            @PathVariable UUID deliveryId
    ) {
        DeliveryDetailResponse response = deliveryQueryUseCase.getDeliveryDetailInternal(deliveryId);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    // 배송 Route 조회 (Notification이 슬랙 메시지 생성 시 호출)
    @SecurityRequirements // internal API - JWT 헤더 불필요
    @GetMapping("/{deliveryId}/routes")
    public ResponseEntity<GeneralResponse<List<RouteResponse>>> getRoutes(
            @PathVariable UUID deliveryId
    ) {
        List<RouteResponse> response = routeQueryUseCase.getRoutesInternal(deliveryId);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }
}
