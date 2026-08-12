package com.sparta.logistics.infrastructure.feign.constant;

/**
 * User&Auth 서비스의 배송 담당자 목록 검색 API(deliveryType 파라미터) 값.
 */
public final class DeliveryManagerType {

    public static final String COMPANY_DELIVERY = "COMPANY_DELIVERY";
    public static final String HUB_DELIVERY = "HUB_DELIVERY";

    private DeliveryManagerType() {
    }
}
