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
        validatePage(page);
        validatePageSize(size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Delivery> result = deliveryRepository.search(status, hubId, currentUserId, role, pageable);

        return DeliveryPageResponse.from(result);
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

    // page가 음수면 PageRequest.of()가 IllegalArgumentException을 던지고,
    // 그게 GlobalExceptionHandler의 Exception 핸들러(500)로 잡혀버린다.
    // 클라이언트 잘못이니 여기서 미리 걸러서 400(INVALID_REQUEST)으로 응답한다.
    private void validatePage(int page) {
        if (page < 0) {
            throw new ApiException(ErrorResponseCode.INVALID_REQUEST, "page는 0 이상이어야 합니다.");
        }
    }

    // size가 10/30/50인지 체크
    private void validatePageSize(int size) {
        if (!ALLOWED_PAGE_SIZES.contains(size)) {
            throw new ApiException(ErrorResponseCode.INVALID_REQUEST, "size는 10, 30, 50 중 하나여야 합니다.");
        }
    }
}
