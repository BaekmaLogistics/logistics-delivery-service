package com.sparta.logistics.infrastructure.feign.dto;

import java.util.UUID;

/**
 * User&Auth 서비스의 "사용자 단건 조회"(GET /internal/api/v1/users/{userId}) 응답 DTO.
 * HUB_MANAGER/COMPANY_MANAGER 권한 스코프 체크에 필요한 hubId/companyId만 사용한다.
 * - hubId : HUB_MANAGER가 담당하는 허브 ID (그 외 역할이면 null일 수 있음)
 * - companyId : COMPANY_MANAGER가 소속된 업체 ID (그 외 역할이면 null일 수 있음)
 */
public record UserInfoResponse(
        UUID userId,
        String role,
        UUID hubId,
        UUID companyId
) {
}
