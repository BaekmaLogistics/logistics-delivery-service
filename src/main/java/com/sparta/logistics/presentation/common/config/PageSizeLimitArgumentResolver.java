package com.sparta.logistics.presentation.common.config;


import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Set;

@Component
public class PageSizeLimitArgumentResolver implements HandlerMethodArgumentResolver {

    private final PageableHandlerMethodArgumentResolver delegate = new PageableHandlerMethodArgumentResolver();

    // 허용할 페이지 사이즈 화이트리스트
    private static final Set<Integer> ALLOWED_SIZES = Set.of(10, 30, 50);
    private static final int DEFAULT_SIZE = 10;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // Pageable 타입의 파라미터만 낚아챔
        return Pageable.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        // 스프링 순정 리졸버를 통해 1차로 Pageable 객체를 파싱해옴
        Pageable pageable = (Pageable) delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);

        // 파싱된 size 값이 허용된 값이 아니라면 기본값(10)으로 보정
        if (!ALLOWED_SIZES.contains(pageable.getPageSize())) {
            return PageRequest.of(pageable.getPageNumber(), DEFAULT_SIZE, pageable.getSort());
        }

        return pageable;
    }
}
