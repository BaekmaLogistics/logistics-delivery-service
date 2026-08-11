package com.sparta.logistics;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// 테스트 실행 시 진짜 Postgres/RabbitMQ를 Docker 컨테이너로 띄워서 컨텍스트를 로드한다.
// @ServiceConnection이 이 컨테이너들의 접속 정보를 application.yml의 datasource/rabbitmq
// 설정 대신 자동으로 주입해준다 - 별도 URL/포트 설정이 필요 없다.
@Testcontainers
@SpringBootTest
class LogisticsApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Test
    void contextLoads() {
    }

}
