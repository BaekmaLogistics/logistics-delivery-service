package com.sparta.logistics.infrastructure.feign.dto;

import java.util.List;
import java.util.UUID;

/**
 * User&Auth 서비스의 "배송 담당자 목록 검색"(GET /api/v1/delivery-managers) 응답 DTO.
 * 주의: User 서비스는 페이지 필드명이 totalPage(단수형)이다. Hub 서비스의 totalPages(복수형)와 다르니 헷갈리지 말 것.
 */
public record DeliveryManagerPageResponse(
        List<DeliveryManagerItem> content,
        int page,
        int size,
        long totalElements,
        int totalPage
) {
    public record DeliveryManagerItem(
            UUID userId,
            String name,
            String slackId,
            String deliveryType,
            UUID hubId,
            Integer deliveryOrder
    ) {
    }
}
