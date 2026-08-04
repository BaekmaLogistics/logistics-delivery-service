# Commit Convention

## 기본 구조

```
분류(스코프): 제목

본문 [선택 사항]

꼬리말 [선택 사항]
```

- 제목만 작성할 때: `git commit -m [커밋 메시지]`
- 본문까지 작성할 때: `git commit`

## 제목

- #이슈번호 포함
- 50자 이하로 제한
- 마침표(.)로 끝나지 않음
- 첫 글자는 대문자로 시작 - 또는 전부 소문자
- 명령형 어조

## 스코프

- 변경된 위치
- 함수가 변경 시 함수 이름
- 메소드 추가 시 class 이름

## 본문

- 제목보다 상세한 커밋의 내용과 이유를 설명
- 제목과 본문 사이에 공백(빈 줄) 존재
- 한 줄에 72자 이하

## 타입

- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `docs`: 문서 관련
- `style`: 코드 포맷팅, 공백, 세미콜론 누락 등 코드에 관련 없는 스타일링 작업
- `refactor`: 코드 리팩토링 (버그 수정, 기능 추가 없는 리팩토링)
- `test`: 테스트 코드 관련
- `build`: 빌드 관련 파일 수정
- `ci`: CI 설정 파일 수정
- `perf`: 성능 개선 차원의 코드 수정
- `chore`: 분류하기 어려운 자잘한 수정

## 예시

```
feat(delivery): 배송 생성 API 구현

배송담당자 라운드로빈 배정 로직 포함.
Hub/User&Auth FeignClient 호출 연동.

Resolves: #12
```

---

# 📎 템플릿 원본 README (참고용)

> `logistics-delivery-service`는 `logistics-template` 레포에서 시작됐습니다.
> README.md는 이 서비스(Delivery)에 맞게 새로 작성했고, 템플릿 원본에 있던
> "서비스 복사 후 설정 변경 가이드"는 참고용으로 여기에 그대로 보존합니다.

## Logistics Service Template

스파르타 물류 시스템(Sparta Logistics System) 마이크로서비스 작성을 위한 공통 Spring Boot 템플릿 레포지토리입니다.

### 🛠 주요 기술 스택 & 포함된 설정
- **Java**: 17
- **Framework**: Spring Boot 3.5.14
- **Database**: PostgreSQL (Spring Data JPA)
- **API Docs**: Springdoc OpenAPI (Swagger UI)
- **Testing**: JUnit 5, Testcontainers

### 📁 프로젝트 패키지 구조
```text
src/main/java/com/sparta/logistics
├── application/       # 비즈니스 유스케이스 / 서비스 로직
├── domain/            # 도메인 엔티티, 리포지토리 인터페이스
├── infrastructure/    # DB, 외부 API 연동 구현체
└── presentation/      # Controller, DTO 및 공통 예외/응답 처리
    └── common/
        ├── dto/       # 공통 응답 포맷 (GeneralResponse, ErrorResponse 등)
        └── exception/ # 공통 예외 핸들러 (GlobalExceptionHandler, ApiException)
```

### ⚙️ 서비스 복사 후 설정 변경 가이드 (필수)

새로운 마이크로서비스 생성 시 아래 파일들의 서비스/아티팩트 명칭을 각 서비스에 맞춰 수정해 주세요.

#### 1. `build.gradle`
- `description`: 서비스 설명/이름 수정 (예: `description = 'user-service'`)
- (필요시) `group` 설정 수정

#### 2. `settings.gradle`
- `rootProject.name`: 프로젝트/아티팩트 이름 수정 (예: `rootProject.name = 'user-service'`)

#### 3. `src/main/resources/application.yml`
- `spring.application.name`: 각 서비스의 애플리케이션 이름으로 수정 (예: `spring.application.name: user-service`)
- `spring.datasource`: 데이터베이스 접속 환경 변수 설정 (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`)

#### 4. 에러 코드 (`ErrorResponseCode.java`) 컨벤션 적용
- `src/main/java/com/sparta/logistics/presentation/common/dto/response/ErrorResponseCode.java`
- 각 서비스에서 발생하는 예외를 구분하기 위해 서비스 접두사(Prefix) 형태의 에러 코드를 추가 정의합니다.
  - 예시:
    - 공통: `COMMON_0001` (서버 오류), `COMMON_0002` (잘못된 요청)
    - 회원 서비스: `USER_0001` (사용자 없음), `USER_0002` (중복된 이메일)
    - 허브 서비스: `HUB_0001` (허브 미존재)

#### 5. 패키지 및 메인 클래스 (선택)
- 기본 패키지(`com.sparta.logistics`) 및 메인 실행 클래스(`LogisticsApplication.java`)를 서비스 역할에 맞게 변경/리팩토링하여 사용합니다.

### 🚀 실행 및 API 문서

#### 빌드 및 실행
```bash
./gradlew bootRun
```

#### Swagger API 문서
애플리케이션 실행 후 접속 URL:
- **Swagger UI**: `http://localhost:8080/api/api-docs`
- **OpenAPI Spec**: `http://localhost:8080/api/api-spec`
