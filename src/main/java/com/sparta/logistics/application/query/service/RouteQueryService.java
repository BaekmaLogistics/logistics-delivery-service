package com.sparta.logistics.application.query.service;

import com.sparta.logistics.application.query.dto.RouteResponse;
import com.sparta.logistics.application.query.usecase.RouteQueryUseCase;
import com.sparta.logistics.common.code.ErrorResponseCode;
import com.sparta.logistics.common.constant.UserRole;
import com.sparta.logistics.common.exception.ApiException;
import com.sparta.logistics.domain.entity.Delivery;
import com.sparta.logistics.domain.repository.DeliveryRepository;
import com.sparta.logistics.infrastructure.feign.client.UserFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteQueryService implements RouteQueryUseCase {

    private final DeliveryRepository deliveryRepository;
    private final UserFeignClient userFeignClient;

    @Override
    public List<RouteResponse> getRoutes(UUID deliveryId, UUID currentUserId, UserRole role) {
        // routes까지 EntityGraph로 한 번에 가져옴 (DeliveryQueryService.getDeliveryDetail과 동일한 조회)
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new ApiException(ErrorResponseCode.DELIVERY_NOT_FOUND));

        // 구간 목록은 "배송 상세를 볼 수 있는 사람"이면 다 볼 수 있음 - 상세조회와 동일한 접근 권한 규칙
        validateAccess(delivery, currentUserId, role);

        // 그 배송에 딸린 구간들을 RouteResponse로 변환해서 리턴
        return delivery.getRoutes().stream()
                .map(RouteResponse::from)
                .toList();
    }

    /**
     * 배송 구간 목록 조회 (internal 전용, Notification 서비스가 호출).
     * getDeliveryStatus와 같은 이유로 권한 체크를 하지 않는다.
     */
    @Override
    public List<RouteResponse> getRoutesInternal(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new ApiException(ErrorResponseCode.DELIVERY_NOT_FOUND));

        return delivery.getRoutes().stream()
                .map(RouteResponse::from)
                .toList();
    }

    // DeliveryQueryService.validateAccess와 동일한 규칙 (상세조회를 볼 수 있으면 구간목록도 볼 수 있음)
    private void validateAccess(Delivery delivery, UUID currentUserId, UserRole role) {
        boolean forbidden = role == UserRole.DELIVERY_DRIVER && !delivery.isAssignedTo(currentUserId);

        if (!forbidden && role == UserRole.HUB_MANAGER) {
            UUID managedHubId = userFeignClient.getUserInfo(currentUserId).data().hubId();
            forbidden = !delivery.getDepartureHubId().equals(managedHubId)
                    && !delivery.getDestinationHubId().equals(managedHubId);
        }

        if (!forbidden && role == UserRole.COMPANY_MANAGER) {
            UUID myCompanyId = userFeignClient.getUserInfo(currentUserId).data().companyId();
            forbidden = !delivery.getCompanyId().equals(myCompanyId);
        }

        if (forbidden) {
            throw new ApiException(ErrorResponseCode.FORBIDDEN);
        }
    }
}
