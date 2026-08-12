package com.sparta.logistics.application.query.usecase;

import com.sparta.logistics.application.query.dto.DeliveryDetailResponse;
import com.sparta.logistics.application.query.dto.DeliveryPageResponse;
import com.sparta.logistics.application.query.dto.DeliveryStatusResponse;
import com.sparta.logistics.common.constant.UserRole;
import com.sparta.logistics.domain.model.DeliveryStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * 배송 조회(상세 + 목록 검색 + internal 상태확인) UseCase.
 * 상세/목록은 읽기 전용이고 역할별 권한 체크 로직이 겹쳐서 하나로 묶는다.
 * 상태확인(getDeliveryStatus)은 Order 서비스가 호출하는 internal 전용이라 권한 체크가 없다.
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

    DeliveryStatusResponse getDeliveryStatus(UUID deliveryId);

    /**
     * 배송 상세 조회 (internal 전용). Notification 서비스가 서버 대 서버로 호출하므로
     * 로그인 사용자 헤더/역할 기반 권한 체크를 하지 않는다.
     */
    DeliveryDetailResponse getDeliveryDetailInternal(UUID deliveryId);
}
