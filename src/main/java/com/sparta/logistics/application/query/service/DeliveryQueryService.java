package com.sparta.logistics.application.query.service;

import com.sparta.logistics.application.query.dto.DeliveryDetailResponse;
import com.sparta.logistics.application.query.dto.DeliveryPageResponse;
import com.sparta.logistics.application.query.usecase.DeliveryQueryUseCase;
import com.sparta.logistics.common.code.ErrorResponseCode;
import com.sparta.logistics.common.constant.UserRole;
import com.sparta.logistics.common.exception.ApiException;
import com.sparta.logistics.domain.entity.Delivery;
import com.sparta.logistics.domain.model.DeliveryStatus;
import com.sparta.logistics.domain.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryQueryService implements DeliveryQueryUseCase {

    // 템플릿 쪽 PageSizeLimitArgumentResolver가 아직 빈 스텁이라, size 검증을 우리가 직접 한다.
    // (나중에 그쪽이 실제로 구현되면 이 검증은 제거하고 그걸 갖다 쓰면 됨)
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 30, 50);

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
            int page,
            int size,
            UUID currentUserId,
            UserRole role
    ) {
        validatePageSize(size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Delivery> result = deliveryRepository.search(status, hubId, currentUserId, role, pageable);

        return DeliveryPageResponse.from(result);
    }

    /**
     * 상세 조회 시 역할별 접근 권한을 확인한다.
     * - DELIVERY_DRIVER : 본인이 전체 배송 담당(companyDriverId)도 아니고, 소속 구간(route) 중
     *   담당(driverId)인 것도 하나 없으면 FORBIDDEN
     * - MASTER/HUB_MANAGER(임시)/COMPANY_MANAGER(임시) : 제한 없음
     */
    private void validateAccess(Delivery delivery, UUID currentUserId, UserRole role) {
        boolean forbidden = role == UserRole.DELIVERY_DRIVER
                && !delivery.getCompanyDriverId().equals(currentUserId)
                && delivery.getRoutes().stream()
                        .noneMatch(route -> route.getDriverId().equals(currentUserId));

        if (forbidden) {
            throw new ApiException(ErrorResponseCode.FORBIDDEN);
        }
    }

    // size가 10/30/50인지 체크
    private void validatePageSize(int size) {
        if (!ALLOWED_PAGE_SIZES.contains(size)) {
            throw new ApiException(ErrorResponseCode.INVALID_REQUEST, "size는 10, 30, 50 중 하나여야 합니다.");
        }
    }
}
