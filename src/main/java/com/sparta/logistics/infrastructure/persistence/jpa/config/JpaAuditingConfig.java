package com.sparta.logistics.infrastructure.persistence.jpa.config;

import com.sparta.logistics.presentation.common.constant.HeaderConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            // 현재 쓰레드의 HTTP 요청 객체 가져오기
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return Optional.empty(); // HTTP 요청 맥락이 아닌 경우 (스케줄러, 배치, Async 쓰레드 등)
            }

            HttpServletRequest request = attributes.getRequest();
            String userIdHeader = request.getHeader(HeaderConstants.USER_ID);

            if (userIdHeader == null || userIdHeader.isBlank()) {
                return Optional.empty(); // 헤더가 없는 요청 (비회원, 회원가입 등)
            }

            try {
                return Optional.of(UUID.fromString(userIdHeader));
            } catch (IllegalArgumentException e) {
                return Optional.empty(); // 올바른 UUID 형식이 아닌 경우
            }
        };
    }

}