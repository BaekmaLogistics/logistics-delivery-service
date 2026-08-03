# Delivery Service

스파르타 물류 시스템(Sparta Logistics System)의 **배송(Delivery) 도메인** 마이크로서비스입니다.

배송/배송경로기록 관리, 배송담당자(허브담당자/업체담당자) 라운드로빈 배정, 배송 상태 추적을 담당합니다.

---

## 🛠 주요 기술 스택
- **Java**: 17
- **Framework**: Spring Boot 3.5.14
- **Database**: PostgreSQL (Spring Data JPA, QueryDSL)
- **Cache**: Redis
- **Messaging**: RabbitMQ
- **Service Discovery / 통신**: Eureka, OpenFeign
- **Tracing**: Zipkin / Micrometer
- **API Docs**: Springdoc OpenAPI (Swagger UI)
- **Testing**: JUnit 5, Testcontainers

---

## 📁 프로젝트 패키지 구조 (CQRS)
```text
src/main/java/com/sparta/logistics
├── application/
│   ├── command/        # 생성/수정/삭제 유스케이스
│   └── query/           # 조회/검색/통계 유스케이스
├── domain/
│   ├── entity/          # JPA 엔티티 (Delivery, DeliveryRoute)
│   ├── model/           # 순수 도메인 모델 (DeliveryStatus, RouteStatus 등)
│   └── repository/      # Repository 인터페이스
├── infrastructure/
│   ├── cache/            # Redis 설정
│   ├── feign/            # Hub, User&Auth FeignClient
│   ├── messaging/        # RabbitMQ 설정/이벤트 발행
│   └── persistence/
│       ├── command/      # 쓰기용 Repository 구현
│       ├── query/        # QueryDSL 기반 조회 Repository 구현
│       └── jpa/           # BaseEntity, JpaAuditingConfig 등 공통 JPA 설정
└── presentation/
    ├── command/controller/  # POST/PATCH/DELETE 컨트롤러
    ├── query/controller/    # GET 컨트롤러
    └── common/               # 공통 응답 포맷, 예외 처리
```

---

## ⚙️ 서비스 설정
- `spring.application.name`: `delivery-service`
- `server.port`: `8083` (팀 컨벤션)
- 에러 코드 Prefix: `DELIVERY_XXXX` (`ErrorResponseCode.java` 참고)

---

## 🚀 실행 및 API 문서

### 빌드 및 실행
```bash
./gradlew bootRun
```

### Swagger API 문서
애플리케이션 실행 후 접속 URL:
- **Swagger UI**: `http://localhost:8083/api/api-docs`
- **OpenAPI Spec**: `http://localhost:8083/api/api-spec`
