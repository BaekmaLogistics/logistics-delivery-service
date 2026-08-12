package com.sparta.logistics.application.query.service;

import com.sparta.logistics.application.query.dto.DeliveryDetailResponse;
import com.sparta.logistics.application.query.dto.DeliveryPageResponse;
import com.sparta.logistics.application.query.dto.DeliveryStatusResponse;
import com.sparta.logistics.application.query.usecase.DeliveryQueryUseCase;
import com.sparta.logistics.common.code.ErrorResponseCode;
import com.sparta.logistics.common.constant.UserRole;
import com.sparta.logistics.common.exception.ApiException;
import com.sparta.logistics.domain.entity.Delivery;
import com.sparta.logistics.domain.model.DeliveryStatus;
import com.sparta.logistics.domain.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryQueryService implements DeliveryQueryUseCase {

    // page/size 검증(10,30,50 외 값은 10으로 보정, 음수 page는 0으로 보정)은 팀 공통 템플릿의
    // PageSizeLimitArgumentResolver + WebConfig(전역 등록)가 컨트롤러 진입 전에 이미 처리해준다.
    // 그래서 여기서 별도 검증 없이 넘어온 Pageable을 그대로 쓴다.
    private final DeliveryRepository deliveryRepository;

    @Override
    public DeliveryDetailResponse getDeliveryDetail(UUID deliveryId, UUID currentUserId, UserRole role) {
        // routes까지 fetch join으로 한 번에 가져옴 (EntityGraph) - 아래 validateAccess에서
        // delivery.getRoutes()를 호출해도 쿼리가 추가로 나가지 않는다.
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new ApiException(ErrorResponseCode.DELIVERY_NOT_FOUND));

        // 권한 체크
        validateAccess(delivery, currentUserId, role);

        return DeliveryDetailResponse.from(delivery);
    }

    @Override
    public DeliveryPageResponse getDeliveries(
            DeliveryStatus status,
            UUID hubId,
            Pageable pageable,
            UUID currentUserId,
            UserRole role
    ) {
        Page<Delivery> result = deliveryRepository.search(status, hubId, currentUserId, role, pageable);

        return DeliveryPageResponse.from(result);
    }

    /**
     * 배송 진행 상태만 가볍게 확인한다 (internal 전용).
     * Order 서비스가 서버 대 서버로 호출하므로 로그인 사용자 헤더/역할 기반 권한 체크를 하지 않는다.
     */
    @Override
    public DeliveryStatusResponse getDeliveryStatus(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new ApiException(ErrorResponseCode.DELIVERY_NOT_FOUND));

        return DeliveryStatusResponse.from(delivery);
    }

    /**
     * 배송 상세 조회 (internal 전용, Notification 서비스가 호출).
     * getDeliveryStatus와 같은 이유로 권한 체크를 하지 않는다.
     */
    @Override
    public DeliveryDetailResponse getDeliveryDetailInternal(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new ApiException(ErrorResponseCode.DELIVERY_NOT_FOUND));

        return DeliveryDetailResponse.from(delivery);
    }

    /**
     * 상세 조회 시 역할별 접근 권한을 확인한다.
     * - DELIVERY_DRIVER : 본인이 담당(Delivery.isAssignedTo)이 아니면 FORBIDDEN
     * - MASTER/HUB_MANAGER(임시)/COMPANY_MANAGER(임시) : 제한 없음
     */
    private void validateAccess(Delivery delivery, UUID currentUserId, UserRole role) {
        boolean forbidden = role == UserRole.DELIVERY_DRIVER && !delivery.isAssignedTo(currentUserId);

        if (forbidden) {
            throw new ApiException(ErrorResponseCode.FORBIDDEN);
        }
    }
}
