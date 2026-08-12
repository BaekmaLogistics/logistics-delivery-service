package com.sparta.logistics.presentation.query.controller;

import com.sparta.logistics.application.query.dto.DeliveryDetailResponse;
import com.sparta.logistics.application.query.dto.DeliveryPageResponse;
import com.sparta.logistics.application.query.dto.RouteResponse;
import com.sparta.logistics.application.query.usecase.DeliveryQueryUseCase;
import com.sparta.logistics.application.query.usecase.RouteQueryUseCase;
import com.sparta.logistics.common.code.GeneralResponseCode;
import com.sparta.logistics.common.constant.UserRole;
import com.sparta.logistics.domain.model.DeliveryStatus;
import com.sparta.logistics.presentation.common.constant.HeaderConstants;
import com.sparta.logistics.presentation.common.dto.response.GeneralResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 배송 조회(상세 + 목록 검색 + 구간 목록) API 컨트롤러.
 * Gateway가 인증을 마친 뒤 X-User-Id / X-User-Role 헤더로 요청자 정보를 내려준다는 전제.
 */
@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryQueryController {

    private final DeliveryQueryUseCase deliveryQueryUseCase;
    private final RouteQueryUseCase routeQueryUseCase;

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
            // size는 팀 공통 PageSizeLimitArgumentResolver가 10/30/50 외 값을 10으로 보정해줌.
            // page 음수 등 나머지는 Spring 기본 Pageable 파싱이 처리(음수는 0으로 보정, 크래시 없음).
            // @ParameterObject: springdoc이 Pageable을 하나의 JSON 객체가 아니라 page/size/sort
            // 개별 쿼리 파라미터로 풀어서 Swagger에 보여주게 함(Swagger에 객체 하나로 뭉쳐 보임).
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader(HeaderConstants.USER_ID) UUID currentUserId,
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole role
    ) {
        DeliveryPageResponse response =
                deliveryQueryUseCase.getDeliveries(status, hubId, pageable, currentUserId, role);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }

    @GetMapping("/{deliveryId}/routes")
    public ResponseEntity<GeneralResponse<List<RouteResponse>>> getRoutes(
            @PathVariable UUID deliveryId,
            @RequestHeader(HeaderConstants.USER_ID) UUID currentUserId,
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole role
    ) {
        List<RouteResponse> response = routeQueryUseCase.getRoutes(deliveryId, currentUserId, role);

        return GeneralResponse.toResponseEntity(GeneralResponseCode.OK, response);
    }
}
