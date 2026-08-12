package com.sparta.logistics.infrastructure.feign.client;

import com.sparta.logistics.infrastructure.feign.config.OpenFeignConfig;
import com.sparta.logistics.infrastructure.feign.dto.DeliveryManagerPageResponse;
import com.sparta.logistics.presentation.common.dto.response.GeneralResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * User&Auth 서비스(logistics-user-service) Feign 클라이언트.
 * name은 Eureka에 등록된 spring.application.name 값과 일치해야 한다.
 */
@FeignClient(name = "user-service", configuration = OpenFeignConfig.class)
public interface UserFeignClient {

    /**
     * 배송 담당자 목록 검색. deliveryType + hubId로 필터링해서 조회한다.
     */
    @GetMapping("/internal/api/v1/delivery-managers")
    GeneralResponse<DeliveryManagerPageResponse> getDeliveryManagers(
            @RequestParam("deliveryType") String deliveryType,
            @RequestParam("hubId") UUID hubId
    );
}
