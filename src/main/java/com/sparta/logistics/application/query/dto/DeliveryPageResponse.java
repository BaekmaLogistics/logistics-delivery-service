package com.sparta.logistics.application.query.dto;

import com.sparta.logistics.domain.entity.Delivery;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 배송 목록 조회(GET /deliveries) 응답 DTO.
 * Hub/User 서비스의 페이지 응답 컨벤션(content, page, size, totalElements, totalPages)을 그대로 따른다.
 */
public record DeliveryPageResponse(
        List<DeliverySummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static DeliveryPageResponse from(Page<Delivery> page) {
        return new DeliveryPageResponse(
                page.getContent().stream()
                        .map(DeliverySummaryResponse::from)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
