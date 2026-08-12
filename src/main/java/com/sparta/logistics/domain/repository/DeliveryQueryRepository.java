package com.sparta.logistics.domain.repository;

import com.sparta.logistics.common.constant.UserRole;
import com.sparta.logistics.domain.entity.Delivery;
import com.sparta.logistics.domain.model.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * QueryDSL로 동적 검색을 수행하는 커스텀 리포지토리.
 * DeliveryRepository(JpaRepository)와는 별도로 두고, Impl에서 QueryDSL 구현체를 붙인다.
 */
// search라는 메서드가 있어야 한다는 껍데기만 정의
public interface DeliveryQueryRepository {

    /**
     * status/hubId 조건(둘 다 선택) + 역할별 "본인 담당" 제한을 적용해서 배송 목록을 검색한다.
     *
     * @param status        선택. null이면 상태 필터 없음
     * @param hubId         선택. null이면 허브 필터 없음, 있으면 출발/도착 허브 둘 중 하나라도 일치
     * @param currentUserId 요청자 ID (X-User-Id)
     * @param role          요청자 역할 (X-User-Role) - DELIVERY_DRIVER는 본인 담당 건만 조회됨
     */
    Page<Delivery> search(
            DeliveryStatus status,
            UUID hubId,
            UUID currentUserId,
            UserRole role,
            Pageable pageable
    );
}
