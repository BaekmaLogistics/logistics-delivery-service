package com.sparta.logistics.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 공통 설정.
 * application.yml에 spring.data.redis.host 값이 없으면(주석 처리된 경우)
 * 이 Config 전체가 비활성화되어 Bean이 등록되지 않음
 * Redis를 사용하지 않는 서비스는 별도 코드 수정 없이 yml의 redis 블록만 주석 처리
 * Cache-Aside 패턴({@code @Cacheable}, {@code @CacheEvict})용 - {@link #cacheManager}
 * Sorted Set 등 자료구조를 직접 다루는 용도 - {@link #redisTemplate}
 *
 * 각 도메인 별로 캐시 유지 시간 설정
 */

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisConfig {

    /**
     * key/캐시 이름 접두어로 사용. 서비스마다 다른 값(company-service, product-service 등)이라
     * 하나의 Redis 인스턴스를 여러 서비스가 공유해도 키가 겹치지 않음
     */
    @Value("${spring.application.name}")
    private String serviceName;

    /**
     * Redis 서버와의 실제 연결을 담당하는 Factory.
     * Lettuce는 Spring Boot 기본 Redis 클라이언트(비동기/스레드 안전).
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port) {
        return new LettuceConnectionFactory(host, port);
    }

    /**
     * Redis 캐싱 전용 ObjectMapper.
     * enericJackson2JsonRedisSerializer를 파라미터 없이 생성하면
     * 내부적으로 기본 ObjectMapper를 새로 만드는데, 여기엔 LocalDateTime/LocalDate 같은
     * Java 8 날짜 타입을 처리하는 JavaTimeModule이 등록되어 있지 않다.
     * 우리 응답 DTO(createdAt, updatedAt 등)에는 LocalDateTime 필드가 반드시 있으므로,
     * 이 상태로 캐싱을 시도하면 InvalidDefinitionException이 발생.
     *
     * 그래서 JavaTimeModule을 명시적으로 등록한 ObjectMapper를 별도로 만들어 주입.
     * - registerModule(new JavaTimeModule()): LocalDateTime 등 직렬화/역직렬화 지원 추가
     * - disable(WRITE_DATES_AS_TIMESTAMPS): 날짜를 숫자(epoch) 대신 ISO-8601 문자열로 저장
     *   (redis-cli로 값을 확인할 때 사람이 읽을 수 있는 형태를 유지하기 위함)
     * - activateDefaultTyping: JSON에 실제 클래스 정보(@class)를 같이 저장해서,
     *   RedisTemplate<String, Object>처럼 값 타입이 Object로 선언된 경우에도
     *   역직렬화 시 원래 클래스로 정확히 복원되도록 함
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 우리 프로젝트 패키지(com.sparta.logistics)에 속한 클래스만 역직렬화 허용
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.sparta.logistics")
                .allowIfSubType("java.util")   // HashMap, ArrayList 등 표준 컬렉션 타입 허용
                .build();

        objectMapper.activateDefaultTyping(
                ptv,
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        return objectMapper;
    }

    /**
     * Cache-Aside용 CacheManager.
     * `@Cacheable` 이 자동으로 캐시 확인, 사실상 String으로 리턴
     * - entryTtl: 캐시 유효시간(TTL). 10분 지나면 자동 만료되어 stale 데이터 방지.
     * - disableCachingNullValues: null은 캐싱하지 않음 (불필요한 캐시 오염 방지 목적).
     * - computePrefixWith: 실제 Redis key = "{serviceName}::{cacheName}::{key값}" 형태로 저장됨.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory cf, ObjectMapper redisObjectMapper) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                // 기본설정에서 시작
                .defaultCacheConfig()
                // 메서드 리턴값이 null이면 캐시에 아예 저장하지 않음.
                .disableCachingNullValues()
                // 기본 캐시 유지 시간 ( 10분이지만 수정 가능 )
                .entryTtl(Duration.ofMinutes(10))
                // 캐시에 저장할 값을 어떻게 바이트로 변환할지 지정
                .serializeValuesWith(
                        // 직렬화
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper)))
                // 형태 지정 저장
                .computePrefixWith(cacheName -> serviceName + "::" + cacheName + "::");

        return RedisCacheManager.builder(cf)
                .cacheDefaults(defaultConfig)
                .build();
    }

    /**
     * Sorted Set(ZSet) 등 자료구조를 직접 조작할 때 사용하는 RedisTemplate.
     * key/value 모두 String/JSON으로 직렬화
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf, ObjectMapper redisObjectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 연결 정보 주입
        template.setConnectionFactory(cf);
        // 일반 key-value 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper));

        // Hash 자료구조 직렬화 (HSET / HGET 등)
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper));
        return template;
    }
}