package com.sparta.logistics.infrastructure.feign.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.logistics.infrastructure.feign.decoder.FeignErrorDecoder;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class OpenFeignConfig {

    @Value("${spring.cloud.openfeign.client.config.default.connect-timeout}")
    private int connectTimeout;

    @Value("${spring.cloud.openfeign.client.config.default.read-timeout}")
    private int readTimeout;

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            connectTimeout,
            TimeUnit.MILLISECONDS,
            readTimeout,
            TimeUnit.MILLISECONDS,
            true
        );
    }

    // 인바운드 요청(Gateway가 붙여준 X-User-Id/X-User-Role)을 그대로 읽어서,
    // 이 서비스가 다른 서비스를 Feign으로 호출할 때도 같은 헤더를 실어 보낸다.
    // 이렇게 해야 A -> B -> C로 이어지는 호출 체인에서도 로그인한 사용자 컨텍스트가 끊기지 않는다.
    @Bean
    public RequestInterceptor userHeaderRequestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();

            String userId = request.getHeader("X-User-Id");
            String userRole = request.getHeader("X-User-Role");

            if (userId != null) {
                requestTemplate.header("X-User-Id", userId);
            }

            if (userRole != null) {
                requestTemplate.header("X-User-Role", userRole);
            }
        };
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder(new ObjectMapper());
    }
}
