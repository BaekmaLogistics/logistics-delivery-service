package com.sparta.logistics.application.query.usecase;

import com.sparta.logistics.application.query.dto.DeliveryDetailResponse;
import com.sparta.logistics.application.query.dto.DeliveryPageResponse;
import com.sparta.logistics.common.constant.UserRole;
import com.sparta.logistics.domain.model.DeliveryStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * 배송 조회(상세 + 목록 검색) UseCase.
 * 둘 다 읽기 전용이고 역할별 권한 체크 로직이 겹쳐서 하나로 묶는다.
 */
// 껍데기(인터페이스)만 정의
// 실제 구현은 DeliveryQueryService
public interface DeliveryQueryUseCase {

    DeliveryDetailResponse getDeliveryDetail(UUID deliveryId, UUID currentUserId, UserRole role);

    DeliveryPageResponse getDeliveries(
            DeliveryStatus status,
            UUID hubId,
            Pageable pageable,
            UUID currentUserId,
            UserRole role
    );
}
