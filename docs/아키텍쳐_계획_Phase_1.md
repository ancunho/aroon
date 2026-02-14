# Phase 1: 기반 구축 상세 가이드

## 개요

Phase 1에서는 실제 비즈니스 로직 개발 전에 **프로젝트의 뼈대**를 완성합니다.
모든 모듈이 공통으로 사용할 기반 코드를 먼저 만들어, 이후 Phase 2~4에서 일관된 패턴으로 개발할 수 있게 합니다.

### Phase 1 작업 목록

| 순서 | 작업 | 생성/수정 파일 | 설명 |
|:----:|------|---------------|------|
| 1-1 | 의존성 추가 | `pom.xml` | Validation 의존성 추가 |
| 1-2 | 통일 응답 래퍼 | `ApiResponse.java` | 모든 API의 응답 포맷 통일 |
| 1-3 | 에러 코드 정의 | `ErrorCode.java` | 에러 코드 enum (다국어 MessageSource 키 연동) |
| 1-4 | 비즈니스 예외 | `BusinessException.java` | 커스텀 예외 클래스 |
| 1-5 | 전역 예외 처리 | `GlobalExceptionHandler.java` | 모든 예외를 ApiResponse로 통일 처리 |
| 1-6 | MyBatis-Plus 설정 | `MybatisPlusConfig.java` | 페이징 플러그인, 자동 채움 핸들러 |
| 1-7 | 다국어 설정 | `MessageConfig.java` + `messages*.properties` | MessageSource + i18n 파일 |
| 1-8 | 웹 설정 | `WebConfig.java` | CORS 설정 |
| 1-9 | application.properties | `application.properties` | 설정 보강 |
| 1-10 | DB 스키마 실행 | `schema.sql` | MySQL에서 스키마 + 초기 데이터 실행 |

### Phase 1 완료 후 디렉토리 구조

```
src/main/java/com/aroon/business/
├── AroonApplication.java                        (기존 - 수정)
├── common/
│   ├── config/
│   │   ├── MybatisPlusConfig.java               (신규)
│   │   ├── MessageConfig.java                   (신규)
│   │   └── WebConfig.java                       (신규)
│   ├── exception/
│   │   ├── ErrorCode.java                       (신규)
│   │   ├── BusinessException.java               (신규)
│   │   └── GlobalExceptionHandler.java          (신규)
│   └── response/
│       └── ApiResponse.java                     (신규)
├── entity/
│   └── User.java                                (기존 - 유지)
├── mapper/
│   └── UserMapper.java                          (기존 - 유지)
└── service/
    ├── UserService.java                         (기존 - 유지)
    └── impl/
        └── UserServiceImpl.java                 (기존 - 유지)

src/main/resources/
├── application.properties                       (기존 - 수정)
├── i18n/
│   ├── messages.properties                      (신규)
│   ├── messages_en.properties                   (신규)
│   └── messages_zh.properties                   (신규)
└── mapper/                                      (XML 매퍼 폴더 - 빈 폴더 생성)
```

---

## Step 1-1. 의존성 추가 (pom.xml)

### 현재 상태

현재 `pom.xml`에 이미 있는 의존성:
- `spring-boot-starter-web` (REST API)
- `spring-boot-starter-thymeleaf` (템플릿)
- `mysql-connector-j` (MySQL 드라이버)
- `mybatis-plus-spring-boot3-starter` (MyBatis-Plus)
- `lombok` (코드 생성)
- `spring-boot-starter-test` (테스트)

### 추가할 의존성

`<dependencies>` 블록 안에 아래 1개를 추가합니다.

```xml
<!-- Validation (입력 검증: @NotBlank, @Size, @Email 등) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### 추가 위치

```xml
<dependencies>
    <!-- 기존 의존성들... -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- ↓↓↓ 여기에 추가 ↓↓↓ -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

### 추가 후 할 일

```bash
# 의존성 다운로드 확인
./mvnw dependency:resolve
```

### 이 의존성이 필요한 이유

Phase 2에서 DTO에 유효성 검증 어노테이션을 사용합니다:

```java
// 예시: UserCreateRequest.java (Phase 2에서 생성)
@NotBlank(message = "{validation.username.required}")  // ← validation 필요
@Size(min = 3, max = 50)                               // ← validation 필요
private String username;
```

> **참고**: Spring Security, JWT 의존성은 Phase 3에서 추가합니다. 지금은 추가하지 않습니다.

---

## Step 1-2. ApiResponse (통일 응답 래퍼)

### 파일 경로

```
src/main/java/com/aroon/business/common/response/ApiResponse.java
```

### 역할

모든 API 응답을 동일한 구조로 감싸는 제네릭 래퍼 클래스입니다.
Controller에서 어떤 데이터를 반환하든 클라이언트는 항상 같은 형태의 JSON을 받게 됩니다.

### 소스 코드

```java
package com.aroon.business.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    // ──────────────────────────────────────
    // 성공 응답
    // ──────────────────────────────────────

    /**
     * 데이터가 있는 성공 응답
     * 예: ApiResponse.success(userResponse)
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("Success")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 메시지를 지정한 성공 응답 (다국어 메시지 사용 시)
     * 예: ApiResponse.success(userResponse, "성공")
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 데이터 없는 성공 응답 (삭제 등)
     * 예: ApiResponse.success()
     */
    public static ApiResponse<Void> success() {
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Success")
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ──────────────────────────────────────
    // 에러 응답
    // ──────────────────────────────────────

    /**
     * 에러 응답
     * 예: ApiResponse.error(404, "사용자를 찾을 수 없습니다")
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
```

### 응답 JSON 예시

**성공 (데이터 있음)**
```json
{
    "code": 200,
    "message": "Success",
    "data": {
        "userSeq": 1,
        "username": "admin",
        "email": "admin@aroon.com"
    },
    "timestamp": "2026-02-13T10:00:00"
}
```

**성공 (데이터 없음 - 삭제 등)**
```json
{
    "code": 200,
    "message": "Success",
    "timestamp": "2026-02-13T10:00:00"
}
```

**에러**
```json
{
    "code": 1001,
    "message": "사용자를 찾을 수 없습니다",
    "timestamp": "2026-02-13T10:00:00"
}
```

### 핵심 포인트

| 항목 | 설명 |
|------|------|
| `@JsonInclude(NON_NULL)` | data가 null이면 JSON에서 아예 제외 (깔끔한 에러 응답) |
| `@Builder` | 빌더 패턴으로 유연한 생성 |
| `@Getter` | Jackson이 JSON 직렬화할 때 getter 필요 |
| 제네릭 `<T>` | 어떤 타입이든 data에 담을 수 있음 |

---

## Step 1-3. ErrorCode (에러 코드 정의)

### 파일 경로

```
src/main/java/com/aroon/business/common/exception/ErrorCode.java
```

### 역할

시스템 전체에서 사용하는 에러 코드를 enum으로 중앙 관리합니다.
`messageKey` 필드를 통해 다국어 MessageSource와 연동됩니다.

### 소스 코드

```java
package com.aroon.business.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ── 공통 (HTTP 표준) ──
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "bad.request"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not.found"),
    INTERNAL_ERROR(500, "internal.error"),

    // ── 사용자 (1xxx) ──
    USER_NOT_FOUND(1001, "user.not.found"),
    DUPLICATE_USERNAME(1002, "user.duplicate.username"),
    DUPLICATE_EMAIL(1003, "user.duplicate.email"),

    // ── 역할 (2xxx) ──
    ROLE_NOT_FOUND(2001, "role.not.found"),
    DUPLICATE_ROLE_CODE(2002, "role.duplicate.code"),

    // ── 권한 (3xxx) ──
    PERMISSION_NOT_FOUND(3001, "permission.not.found"),

    // ── 메뉴 (4xxx) ──
    MENU_NOT_FOUND(4001, "menu.not.found"),

    // ── 인증 (5xxx) ── (Phase 3에서 사용)
    INVALID_CREDENTIALS(5001, "auth.invalid.credentials"),
    TOKEN_EXPIRED(5002, "auth.token.expired"),
    INVALID_TOKEN(5003, "auth.token.invalid");

    /**
     * 응답 코드 (JSON의 "code" 필드)
     */
    private final int code;

    /**
     * 다국어 메시지 키 (messages.properties의 키와 매핑)
     * 예: "user.not.found" → messages.properties에서 해당 키의 값을 조회
     */
    private final String messageKey;
}
```

### 설계 포인트

```
ErrorCode.USER_NOT_FOUND
    ├── code: 1001              → ApiResponse의 "code" 필드
    └── messageKey: "user.not.found"  → messages.properties에서 조회
                                         ├─ (ko) "사용자를 찾을 수 없습니다"
                                         ├─ (en) "User not found"
                                         └─ (zh) "找不到用户"
```

### 에러 코드 체계

| 범위 | 도메인 | 예시 |
|------|--------|------|
| 200 | 성공 | 정상 처리 |
| 400~500 | HTTP 표준 | 잘못된 요청, 인증 필요, 서버 오류 |
| 1000~1999 | 사용자 | USER_NOT_FOUND, DUPLICATE_USERNAME |
| 2000~2999 | 역할 | ROLE_NOT_FOUND, DUPLICATE_ROLE_CODE |
| 3000~3999 | 권한 | PERMISSION_NOT_FOUND |
| 4000~4999 | 메뉴 | MENU_NOT_FOUND |
| 5000~5999 | 인증 | INVALID_CREDENTIALS, TOKEN_EXPIRED |

> 새로운 도메인이 추가되면 해당 범위에 에러 코드를 추가하면 됩니다.

---

## Step 1-4. BusinessException (비즈니스 예외)

### 파일 경로

```
src/main/java/com/aroon/business/common/exception/BusinessException.java
```

### 역할

비즈니스 로직에서 발생하는 예외를 담당합니다.
`ErrorCode`를 포함하고 있어, 예외 발생 시 에러 코드와 메시지를 함께 전달합니다.

### 소스 코드

```java
package com.aroon.business.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * ErrorCode만으로 생성 (메시지는 GlobalExceptionHandler에서 MessageSource로 조회)
     * 예: throw new BusinessException(ErrorCode.USER_NOT_FOUND)
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
    }

    /**
     * ErrorCode + 커스텀 메시지로 생성 (MessageSource 대신 직접 메시지 지정)
     * 예: throw new BusinessException(ErrorCode.BAD_REQUEST, "username은 3자 이상이어야 합니다")
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
```

### 사용 예시 (Phase 2에서 이렇게 사용됨)

```java
// Service 구현체에서 사용
@Override
public UserResponse getUserBySeq(Long userSeq) {
    User user = userMapper.selectById(userSeq);
    if (user == null) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);  // ← 이렇게 던지면
    }
    return toResponse(user);
}
// → GlobalExceptionHandler가 잡아서 → MessageSource로 다국어 메시지 조회 → ApiResponse로 응답
```

### 예외 처리 흐름도

```
Service에서 예외 발생
    throw new BusinessException(ErrorCode.USER_NOT_FOUND)
        │
        ▼
GlobalExceptionHandler가 잡음
    @ExceptionHandler(BusinessException.class)
        │
        ├── errorCode.getMessageKey() → "user.not.found"
        │
        ├── MessageSource 조회 (Accept-Language 헤더 기준)
        │       ├── ko → "사용자를 찾을 수 없습니다"
        │       ├── en → "User not found"
        │       └── zh → "找不到用户"
        │
        └── ApiResponse.error(1001, "사용자를 찾을 수 없습니다") 반환
```

---

## Step 1-5. GlobalExceptionHandler (전역 예외 처리)

### 파일 경로

```
src/main/java/com/aroon/business/common/exception/GlobalExceptionHandler.java
```

### 역할

애플리케이션 전체에서 발생하는 예외를 한 곳에서 처리합니다.
어떤 예외가 발생하든 클라이언트는 항상 `ApiResponse` 형태의 JSON을 받게 됩니다.

### 소스 코드

```java
package com.aroon.business.common.exception;

import com.aroon.business.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Locale;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    // ──────────────────────────────────────
    // 1. 비즈니스 예외 (우리가 직접 던지는 예외)
    // ──────────────────────────────────────
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        Locale locale = LocaleContextHolder.getLocale();

        // MessageSource에서 다국어 메시지 조회
        String message = messageSource.getMessage(
                errorCode.getMessageKey(),
                null,
                e.getMessage(),   // fallback: MessageSource에 키가 없으면 이 값 사용
                locale
        );

        log.warn("BusinessException: code={}, message={}", errorCode.getCode(), message);

        return ResponseEntity
                .status(resolveHttpStatus(errorCode))
                .body(ApiResponse.error(errorCode.getCode(), message));
    }

    // ──────────────────────────────────────
    // 2. 유효성 검증 예외 (@Valid 실패 시)
    // ──────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("유효성 검증 실패");

        log.warn("ValidationException: {}", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST.getCode(), message));
    }

    // ──────────────────────────────────────
    // 3. 404 Not Found (존재하지 않는 URL 접근)
    // ──────────────────────────────────────
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("not.found", null, "Resource not found", locale);

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, message));
    }

    // ──────────────────────────────────────
    // 4. 기타 모든 예외 (예상치 못한 오류)
    // ──────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("internal.error", null, "Internal server error", locale);

        log.error("Unhandled Exception: ", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), message));
    }

    // ──────────────────────────────────────
    // ErrorCode → HTTP 상태 코드 매핑
    // ──────────────────────────────────────
    private HttpStatus resolveHttpStatus(ErrorCode errorCode) {
        return switch (errorCode.getCode()) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            default -> {
                // 1000번대 이상의 비즈니스 에러는 HTTP 200으로 응답
                // (HTTP 상태는 성공이지만, body의 code로 에러를 구분)
                // 또는 HttpStatus.BAD_REQUEST로 통일할 수도 있음 → 팀 협의 사항
                if (errorCode.getCode() >= 1000) {
                    yield HttpStatus.OK;
                }
                yield HttpStatus.INTERNAL_SERVER_ERROR;
            }
        };
    }
}
```

### 처리하는 예외 유형

| 예외 | 발생 시점 | 응답 예시 |
|------|----------|----------|
| `BusinessException` | Service에서 `throw new BusinessException(...)` | `{"code": 1001, "message": "사용자를 찾을 수 없습니다"}` |
| `MethodArgumentNotValidException` | Controller에서 `@Valid` 검증 실패 | `{"code": 400, "message": "사용자명은 필수입니다"}` |
| `NoResourceFoundException` | 존재하지 않는 URL 접근 | `{"code": 404, "message": "리소스를 찾을 수 없습니다"}` |
| `Exception` | 예상치 못한 서버 오류 | `{"code": 500, "message": "서버 내부 오류입니다"}` |

### HTTP 상태 코드 전략 설명

비즈니스 에러(1000번대 이상)의 HTTP 상태 코드를 어떻게 할지는 두 가지 방식이 있습니다:

| 방식 | HTTP Status | body.code | 설명 |
|------|-------------|-----------|------|
| 방식 A (현재) | `200 OK` | `1001` | HTTP는 성공, 비즈니스 에러는 code로 구분 |
| 방식 B | `400 Bad Request` | `1001` | 에러면 HTTP도 에러 |

현재는 **방식 A**로 설정되어 있습니다. `resolveHttpStatus()` 메서드에서 변경 가능합니다.

---

## Step 1-6. MybatisPlusConfig (MyBatis-Plus 설정)

### 파일 경로

```
src/main/java/com/aroon/business/common/config/MybatisPlusConfig.java
```

### 역할

1. **페이징 플러그인**: `selectPage()` 사용 시 자동으로 `LIMIT/OFFSET` SQL 생성
2. **자동 채움 핸들러**: `INSERT` 시 `created_at`, `updated_at` 자동 설정 / `UPDATE` 시 `updated_at` 자동 갱신

### 소스 코드

```java
package com.aroon.business.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class MybatisPlusConfig {

    /**
     * 페이징 플러그인
     *
     * 이 설정이 있어야 MyBatis-Plus의 Page 객체를 사용한 페이징이 동작합니다.
     * selectPage() 호출 시 자동으로 COUNT 쿼리 + LIMIT/OFFSET이 추가됩니다.
     *
     * 사용 예시 (Phase 2에서):
     *   Page<User> page = new Page<>(1, 10);  // 1페이지, 10건
     *   userMapper.selectPage(page, null);
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 자동 채움 핸들러
     *
     * Entity의 @TableField(fill = FieldFill.INSERT) 또는 @TableField(fill = FieldFill.INSERT_UPDATE)
     * 어노테이션이 붙은 필드에 자동으로 값을 채워줍니다.
     *
     * INSERT 시: createdAt, updatedAt 자동 설정
     * UPDATE 시: updatedAt만 자동 갱신
     *
     * Entity에서의 사용 (Phase 2에서):
     *   @TableField(fill = FieldFill.INSERT)
     *   private LocalDateTime createdAt;
     *
     *   @TableField(fill = FieldFill.INSERT_UPDATE)
     *   private LocalDateTime updatedAt;
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime::now, LocalDateTime.class);
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime::now, LocalDateTime.class);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime::now, LocalDateTime.class);
            }
        };
    }
}
```

### 자동 채움이 동작하려면 (Phase 2에서 Entity 수정 시)

Entity에 `@TableField(fill = ...)` 어노테이션을 추가해야 합니다:

```java
// Phase 2에서 User.java를 이렇게 수정할 예정
@TableField(fill = FieldFill.INSERT)
private LocalDateTime createdAt;

@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updatedAt;
```

현재 User.java에는 createdAt, updatedAt 필드가 없으므로 Phase 2에서 추가합니다.
지금은 Config만 먼저 만들어 두세요.

### 페이징 사용 예시 (Phase 2에서)

```java
// Service에서 페이징 조회
public IPage<UserResponse> getUsers(int pageNum, int pageSize) {
    Page<User> page = new Page<>(pageNum, pageSize);
    IPage<User> result = userMapper.selectPage(page, null);
    // result.getRecords() → 데이터 목록
    // result.getTotal()   → 전체 건수
    // result.getPages()   → 전체 페이지 수
    ...
}
```

---

## Step 1-7. 다국어 설정 (MessageConfig + i18n 파일)

### 1-7-A. MessageConfig.java

#### 파일 경로

```
src/main/java/com/aroon/business/common/config/MessageConfig.java
```

#### 역할

Spring의 `MessageSource`를 설정하여 다국어 메시지 파일을 로딩합니다.
클라이언트의 `Accept-Language` 헤더를 기반으로 언어를 자동 결정합니다.

#### 소스 코드

```java
package com.aroon.business.common.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration
public class MessageConfig {

    /**
     * 다국어 메시지 소스 설정
     *
     * src/main/resources/i18n/ 디렉토리에서 메시지 파일을 로딩합니다.
     *   - messages.properties      → 기본 (한국어)
     *   - messages_en.properties   → 영어
     *   - messages_zh.properties   → 중국어
     *
     * 사용 방법:
     *   messageSource.getMessage("user.not.found", null, locale)
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setCacheSeconds(60);  // 60초마다 파일 변경 체크
        return source;
    }

    /**
     * 로케일 결정 전략
     *
     * 클라이언트의 Accept-Language 헤더를 읽어 Locale을 결정합니다.
     *   - Accept-Language: ko  → 한국어
     *   - Accept-Language: en  → 영어
     *   - Accept-Language: zh  → 중국어
     *   - 헤더 없음            → 기본값 한국어
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.KOREAN);
        resolver.setSupportedLocales(List.of(
                Locale.KOREAN,
                Locale.ENGLISH,
                Locale.CHINESE
        ));
        return resolver;
    }
}
```

### 1-7-B. 메시지 파일 (i18n)

3개의 properties 파일을 생성합니다.

#### 파일 경로

```
src/main/resources/i18n/messages.properties       ← 기본 (한국어)
src/main/resources/i18n/messages_en.properties     ← 영어
src/main/resources/i18n/messages_zh.properties     ← 중국어
```

#### messages.properties (한국어 - 기본)

```properties
# =====================================================
# Aroon 다국어 메시지 - 한국어 (기본)
# =====================================================

# ── 공통 ──
success=성공
bad.request=잘못된 요청입니다
unauthorized=인증이 필요합니다
forbidden=접근 권한이 없습니다
not.found=리소스를 찾을 수 없습니다
internal.error=서버 내부 오류입니다

# ── 사용자 ──
user.not.found=사용자를 찾을 수 없습니다
user.duplicate.username=이미 존재하는 사용자명입니다
user.duplicate.email=이미 존재하는 이메일입니다

# ── 역할 ──
role.not.found=역할을 찾을 수 없습니다
role.duplicate.code=이미 존재하는 역할 코드입니다

# ── 권한 ──
permission.not.found=권한을 찾을 수 없습니다

# ── 메뉴 ──
menu.not.found=메뉴를 찾을 수 없습니다

# ── 인증 ──
auth.invalid.credentials=아이디 또는 비밀번호가 올바르지 않습니다
auth.token.expired=토큰이 만료되었습니다
auth.token.invalid=유효하지 않은 토큰입니다

# ── 유효성 검증 ──
validation.username.required=사용자명은 필수입니다
validation.username.size=사용자명은 {min}자 이상 {max}자 이하여야 합니다
validation.password.required=비밀번호는 필수입니다
validation.password.size=비밀번호는 {min}자 이상이어야 합니다
validation.email.format=이메일 형식이 올바르지 않습니다
validation.role.code.required=역할 코드는 필수입니다
validation.role.name.required=역할 이름은 필수입니다
validation.menu.name.required=메뉴 이름은 필수입니다
validation.menu.code.required=메뉴 코드는 필수입니다
validation.menu.type.required=메뉴 유형은 필수입니다
```

#### messages_en.properties (영어)

```properties
# =====================================================
# Aroon i18n Messages - English
# =====================================================

# ── Common ──
success=Success
bad.request=Bad request
unauthorized=Authentication required
forbidden=Access denied
not.found=Resource not found
internal.error=Internal server error

# ── User ──
user.not.found=User not found
user.duplicate.username=Username already exists
user.duplicate.email=Email already exists

# ── Role ──
role.not.found=Role not found
role.duplicate.code=Role code already exists

# ── Permission ──
permission.not.found=Permission not found

# ── Menu ──
menu.not.found=Menu not found

# ── Auth ──
auth.invalid.credentials=Invalid username or password
auth.token.expired=Token has expired
auth.token.invalid=Invalid token

# ── Validation ──
validation.username.required=Username is required
validation.username.size=Username must be between {min} and {max} characters
validation.password.required=Password is required
validation.password.size=Password must be at least {min} characters
validation.email.format=Invalid email format
validation.role.code.required=Role code is required
validation.role.name.required=Role name is required
validation.menu.name.required=Menu name is required
validation.menu.code.required=Menu code is required
validation.menu.type.required=Menu type is required
```

#### messages_zh.properties (중국어)

```properties
# =====================================================
# Aroon 国际化消息 - 中文
# =====================================================

# ── 通用 ──
success=成功
bad.request=请求错误
unauthorized=需要认证
forbidden=没有访问权限
not.found=未找到资源
internal.error=服务器内部错误

# ── 用户 ──
user.not.found=找不到用户
user.duplicate.username=用户名已存在
user.duplicate.email=邮箱已存在

# ── 角色 ──
role.not.found=找不到角色
role.duplicate.code=角色代码已存在

# ── 权限 ──
permission.not.found=找不到权限

# ── 菜单 ──
menu.not.found=找不到菜单

# ── 认证 ──
auth.invalid.credentials=用户名或密码不正确
auth.token.expired=令牌已过期
auth.token.invalid=无效的令牌

# ── 验证 ──
validation.username.required=用户名为必填项
validation.username.size=用户名长度必须在{min}到{max}个字符之间
validation.password.required=密码为必填项
validation.password.size=密码长度不能少于{min}个字符
validation.email.format=邮箱格式不正确
validation.role.code.required=角色代码为必填项
validation.role.name.required=角色名称为必填项
validation.menu.name.required=菜单名称为必填项
validation.menu.code.required=菜单代码为必填项
validation.menu.type.required=菜单类型为必填项
```

### 다국어 연동 확인 방법

서버 기동 후 동일한 API를 헤더만 바꿔서 호출해보세요:

```bash
# 한국어 (기본)
curl -H "Accept-Language: ko" http://localhost:8080/api/users/999

# 영어
curl -H "Accept-Language: en" http://localhost:8080/api/users/999

# 중국어
curl -H "Accept-Language: zh" http://localhost:8080/api/users/999
```

기대 응답:
```json
// ko
{"code": 1001, "message": "사용자를 찾을 수 없습니다", "timestamp": "..."}

// en
{"code": 1001, "message": "User not found", "timestamp": "..."}

// zh
{"code": 1001, "message": "找不到用户", "timestamp": "..."}
```

---

## Step 1-8. WebConfig (웹 설정)

### 파일 경로

```
src/main/java/com/aroon/business/common/config/WebConfig.java
```

### 역할

CORS(Cross-Origin Resource Sharing) 설정을 담당합니다.
프론트엔드(별도 도메인/포트)에서 API를 호출할 수 있게 합니다.

### 소스 코드

```java
package com.aroon.business.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * CORS 설정
     *
     * 프론트엔드 개발 시 localhost:3000 등에서 API 호출이 가능하도록 허용합니다.
     * 운영 배포 시에는 allowedOrigins를 실제 도메인으로 제한하세요.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")            // 운영 시 실제 도메인으로 변경
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);                   // preflight 캐시 1시간
    }
}
```

> **참고**: Phase 3에서 Spring Security를 적용하면 SecurityConfig에서도 CORS 설정이 필요합니다.
> 그때 이 설정과 SecurityConfig의 CORS 설정을 통합하게 됩니다.

---

## Step 1-9. application.properties 수정

### 파일 경로

```
src/main/resources/application.properties
```

### 현재 내용 → 수정 내용

현재 `application.properties`를 아래 내용으로 **교체**합니다.
기존 내용을 유지하면서 다국어 설정과 서버 설정을 추가합니다.

```properties
# =====================================================
# Aroon Application Configuration
# =====================================================

spring.application.name=aroon

# ── 서버 ──
server.port=8080

# ── MySQL Database ──
spring.datasource.url=jdbc:mysql://localhost:3306/aroon?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ── MyBatis-Plus ──
mybatis-plus.mapper-locations=classpath:/mapper/**/*.xml
mybatis-plus.type-aliases-package=com.aroon.business.entity
mybatis-plus.configuration.map-underscore-to-camel-case=true
mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl

# ── 다국어 (MessageSource) ──
spring.messages.basename=i18n/messages
spring.messages.encoding=UTF-8
spring.messages.cache-duration=60

# ── Jackson (JSON 응답 설정) ──
spring.jackson.time-zone=Asia/Seoul
spring.jackson.date-format=yyyy-MM-dd HH:mm:ss
spring.jackson.serialization.write-dates-as-timestamps=false
```

### 추가된 설정 설명

| 설정 | 값 | 설명 |
|------|------|------|
| `server.port` | 8080 | 서버 포트 명시 |
| `spring.messages.basename` | i18n/messages | 다국어 파일 경로 (MessageConfig와 연동) |
| `spring.messages.encoding` | UTF-8 | 한국어/중국어 깨짐 방지 |
| `spring.messages.cache-duration` | 60 | 메시지 파일 캐시 60초 |
| `spring.jackson.time-zone` | Asia/Seoul | JSON의 날짜/시간 타임존 |
| `spring.jackson.date-format` | yyyy-MM-dd HH:mm:ss | 날짜 포맷 |
| `spring.jackson.serialization.write-dates-as-timestamps` | false | 날짜를 timestamp 대신 문자열로 출력 |

> **주의**: `spring.datasource.password=your_password` 부분은 실제 MySQL 비밀번호로 변경하세요.

---

## Step 1-10. DB 스키마 실행

### 실행 파일

```
docs/schema.sql
```

### 실행 방법

MySQL 클라이언트에서 `docs/schema.sql` 파일을 실행합니다.

```bash
# 방법 1: MySQL CLI
mysql -u root -p < docs/schema.sql

# 방법 2: MySQL Workbench
# → File → Open SQL Script → docs/schema.sql 선택 → 실행

# 방법 3: IntelliJ Database 탭
# → Data Sources에서 MySQL 연결 → schema.sql 우클릭 → Run
```

### 실행 결과 확인

```sql
USE aroon;

-- 테이블 확인 (7개 테이블이 보여야 함)
SHOW TABLES;
-- 결과: user, role, permission, menu, user_role, role_permission, role_menu

-- 초기 데이터 확인
SELECT * FROM role;           -- 3건 (ADMIN, MANAGER, USER)
SELECT * FROM permission;     -- 16건
SELECT * FROM menu;           -- 5건
SELECT * FROM user;           -- 1건 (admin)
SELECT * FROM user_role;      -- 1건 (admin → ROLE_ADMIN)
SELECT * FROM role_permission; -- ADMIN 16건 + MANAGER 12건 + USER 4건 = 32건
SELECT * FROM role_menu;      -- 5건 (ADMIN에게 전체 메뉴)
```

### 다국어 번역 테이블 (추가)

`다국어기획.md`에서 설계한 `translation` 테이블도 함께 생성합니다.
아래 SQL을 `schema.sql` 실행 후 추가로 실행하세요.

```sql
-- =====================================================
-- 8. 다국어 번역 테이블
-- =====================================================
DROP TABLE IF EXISTS `translation`;
CREATE TABLE `translation` (
    `translation_seq` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '번역 SEQ',
    `table_name`      VARCHAR(50)  NOT NULL COMMENT '대상 테이블명',
    `column_name`     VARCHAR(50)  NOT NULL COMMENT '대상 컬럼명',
    `record_seq`      BIGINT       NOT NULL COMMENT '대상 레코드 SEQ',
    `locale`          VARCHAR(10)  NOT NULL COMMENT '언어 코드 (ko, en, zh)',
    `value`           TEXT         NOT NULL COMMENT '번역 값',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (`translation_seq`),
    UNIQUE KEY `uk_translation` (`table_name`, `column_name`, `record_seq`, `locale`),
    KEY `idx_translation_lookup` (`table_name`, `record_seq`, `locale`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='다국어 번역';

-- 메뉴 이름 번역
INSERT INTO `translation` (`table_name`, `column_name`, `record_seq`, `locale`, `value`) VALUES
('menu', 'menu_name', 1, 'ko', '시스템 관리'),
('menu', 'menu_name', 1, 'en', 'System Management'),
('menu', 'menu_name', 1, 'zh', '系统管理'),
('menu', 'menu_name', 2, 'ko', '사용자 관리'),
('menu', 'menu_name', 2, 'en', 'User Management'),
('menu', 'menu_name', 2, 'zh', '用户管理'),
('menu', 'menu_name', 3, 'ko', '역할 관리'),
('menu', 'menu_name', 3, 'en', 'Role Management'),
('menu', 'menu_name', 3, 'zh', '角色管理'),
('menu', 'menu_name', 4, 'ko', '권한 관리'),
('menu', 'menu_name', 4, 'en', 'Permission Management'),
('menu', 'menu_name', 4, 'zh', '权限管理'),
('menu', 'menu_name', 5, 'ko', '메뉴 관리'),
('menu', 'menu_name', 5, 'en', 'Menu Management'),
('menu', 'menu_name', 5, 'zh', '菜单管理');

-- 역할 이름 번역
INSERT INTO `translation` (`table_name`, `column_name`, `record_seq`, `locale`, `value`) VALUES
('role', 'role_name', 1, 'ko', '관리자'),
('role', 'role_name', 1, 'en', 'Administrator'),
('role', 'role_name', 1, 'zh', '管理员'),
('role', 'role_name', 2, 'ko', '매니저'),
('role', 'role_name', 2, 'en', 'Manager'),
('role', 'role_name', 2, 'zh', '经理'),
('role', 'role_name', 3, 'ko', '일반 사용자'),
('role', 'role_name', 3, 'en', 'User'),
('role', 'role_name', 3, 'zh', '普通用户');
```

---

## 체크리스트

Phase 1 완료 후 아래 항목을 확인하세요.

### 파일 생성 체크

| # | 파일 | 확인 |
|---|------|------|
| 1 | `pom.xml` — validation 의존성 추가됨 | ☐ |
| 2 | `common/response/ApiResponse.java` | ☐ |
| 3 | `common/exception/ErrorCode.java` | ☐ |
| 4 | `common/exception/BusinessException.java` | ☐ |
| 5 | `common/exception/GlobalExceptionHandler.java` | ☐ |
| 6 | `common/config/MybatisPlusConfig.java` | ☐ |
| 7 | `common/config/MessageConfig.java` | ☐ |
| 8 | `common/config/WebConfig.java` | ☐ |
| 9 | `src/main/resources/i18n/messages.properties` | ☐ |
| 10 | `src/main/resources/i18n/messages_en.properties` | ☐ |
| 11 | `src/main/resources/i18n/messages_zh.properties` | ☐ |
| 12 | `application.properties` — 설정 추가됨 | ☐ |
| 13 | DB — schema.sql 실행 완료 (7개 테이블 + 초기 데이터) | ☐ |
| 14 | DB — translation 테이블 생성 + 초기 데이터 | ☐ |

### 빌드 확인

```bash
# 컴파일 확인 (테스트 제외)
./mvnw compile -DskipTests

# 에러 없이 BUILD SUCCESS가 나와야 합니다
```

### 기동 확인

```bash
# 서버 기동 (DB 연결이 되어야 정상 기동)
./mvnw spring-boot:run
```

기동 시 콘솔에 아래와 같은 로그가 보이면 성공:
```
Started AroonApplication in X.XXX seconds
```

---

## 다음 단계: Phase 2 미리보기

Phase 1이 완료되면 Phase 2에서는 이 기반 위에 실제 CRUD를 구현합니다.

| 작업 | 설명 |
|------|------|
| Entity 수정/생성 | User 수정 + Role, Permission, Menu, 관계 엔티티 신규 생성 |
| Mapper 생성 | 7개 Mapper 인터페이스 (BaseMapper 상속) |
| DTO 생성 | Request/Response DTO + 유효성 검증 |
| Service 구현 | CRUD 비즈니스 로직 |
| Controller 구현 | REST API 엔드포인트 |

Phase 1에서 만든 `ApiResponse`, `ErrorCode`, `BusinessException`, `GlobalExceptionHandler`를
Phase 2의 모든 Controller와 Service에서 사용하게 됩니다.
