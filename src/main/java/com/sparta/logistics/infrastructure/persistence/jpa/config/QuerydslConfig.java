package com.sparta.logistics.infrastructure.persistence.jpa.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QueryDSL이 동적 쿼리를 만들 때 쓰는 JPAQueryFactory를 빈으로 등록한다.
 * EntityManager는 스프링이 트랜잭션마다 실제 영속성 컨텍스트로 바꿔서 주입해주는 프록시라서
 * 여기서 필드로 주입받아도 안전하다.
 */
@Configuration
public class QuerydslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
