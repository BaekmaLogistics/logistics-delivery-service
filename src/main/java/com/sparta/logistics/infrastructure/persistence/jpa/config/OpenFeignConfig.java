package com.sparta.logistics.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.logistics.infrastructure.feign.decoder.FeignErrorDecoder;
import feign.Logger;
import feign.Request;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder(new ObjectMapper());
    }
}
