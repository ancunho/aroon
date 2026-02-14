# Phase 4: 관계 매핑 & 고급 기능 상세 가이드

## 개요

Phase 3에서 완성한 인증/인가 위에 **RBAC 관계 할당**(사용자-역할, 역할-권한, 역할-메뉴),
**사용자별 메뉴 조회**, **페이징 처리**, **동적 콘텐츠 다국어**를 구현합니다.

### 전제 조건
- Phase 1, Phase 2, Phase 3의 모든 Step이 완료되어 있어야 합니다
- DB에 7개 RBAC 테이블 + translation 테이블이 존재해야 합니다
- JWT 인증/인가가 정상 동작해야 합니다

### Phase 4 작업 목록

| 순서 | 작업 | 생성/수정 파일 | 설명 |
|:----:|------|---------------|------|
| 4-1 | 페이징 공통 DTO | 신규 2개 | PageRequest, PageResponse |
| 4-2 | Translation 엔티티 + Mapper | 신규 3개 | Entity, Mapper, XML 매퍼 |
| 4-3 | TranslationHelper (캐싱 포함) | 신규 1개 | 번역 공통 유틸리티 + 서버 기동 시 전체 로딩 |
| 4-4 | 사용자-역할 할당 API | 수정 3개, 신규 1개 | 기존 UserService/Impl/Controller 수정 |
| 4-5 | 역할-권한 할당 API | 수정 3개, 신규 1개 | 기존 RoleService/Impl/Controller 수정 |
| 4-6 | 역할-메뉴 할당 API | 수정 3개 | 기존 RoleService/Impl/Controller 수정 |
| 4-7 | 사용자 상세 조회 확장 | 수정 4개, 신규 1개 | 역할 포함 상세 조회 |
| 4-8 | 역할 상세 조회 확장 | 수정 4개, 신규 1개 | 권한/메뉴 포함 상세 조회 |
| 4-9 | 사용자별 메뉴 트리 조회 | 수정 3개 | 로그인 사용자의 메뉴만 반환 |
| 4-10 | 페이징 적용 | 수정 8개 | 목록 조회에 페이징 적용 |
| 4-11 | 동적 콘텐츠 다국어 적용 | 수정 3개 | 기본 언어(ko) 스킵 + 메뉴/역할/권한 이름 번역 |
| 4-12 | 번역 관리 CRUD API | 신규 4개 | 번역 관리용 API + 캐시 무효화 (선택) |

### Phase 4 완료 후 디렉토리 구조 (신규/수정 파일만 표시)

```
src/main/java/com/aroon/business/
│
├── common/util/
│   └── TranslationHelper.java               (신규)
│
├── controller/
│   ├── UserController.java                   (수정 - 역할 할당, 페이징, 상세 조회)
│   ├── RoleController.java                   (수정 - 권한/메뉴 할당, 페이징, 상세 조회)
│   ├── PermissionController.java             (수정 - 페이징)
│   ├── MenuController.java                   (수정 - 사용자별 메뉴, 페이징)
│   └── TranslationController.java            (신규 - 선택)
│
├── dto/
│   ├── request/
│   │   ├── RoleAssignRequest.java            (신규)
│   │   ├── PermissionAssignRequest.java      (신규)
│   │   └── MenuAssignRequest.java            (신규)
│   └── response/
│       ├── UserDetailResponse.java           (신규)
│       ├── RoleDetailResponse.java           (신규)
│       ├── PageResponse.java                 (신규)
│       └── TranslationResponse.java          (신규 - 선택)
│
├── entity/
│   └── Translation.java                      (신규)
│
├── mapper/
│   ├── UserMapper.java                       (수정 - 역할 조회 메서드 추가)
│   ├── RoleMapper.java                       (수정 - 권한/메뉴 조회 메서드 추가)
│   ├── MenuMapper.java                       (수정 - 사용자별 메뉴 조회 추가)
│   └── TranslationMapper.java               (신규)
│
├── service/
│   ├── UserService.java                      (수정)
│   ├── RoleService.java                      (수정)
│   ├── MenuService.java                      (수정)
│   ├── TranslationService.java               (신규 - 선택)
│   └── impl/
│       ├── UserServiceImpl.java              (수정)
│       ├── RoleServiceImpl.java              (수정)
│       ├── PermissionServiceImpl.java        (수정)
│       ├── MenuServiceImpl.java              (수정)
│       └── TranslationServiceImpl.java       (신규 - 선택)
│
src/main/resources/mapper/
├── UserMapper.xml                            (수정 - 역할 조인 추가)
├── RoleMapper.xml                            (신규 - 권한/메뉴 조인)
├── MenuMapper.xml                            (수정 - 사용자별 메뉴 조인)
└── TranslationMapper.xml                     (신규)
```

---

## Step 4-1. 페이징 공통 DTO

### 역할

MyBatis-Plus의 `Page` 객체를 활용하되, API 요청/응답에는 우리만의 DTO를 사용합니다.

### 4-1-A. PageResponse.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/dto/response/PageResponse.java`

> MyBatis-Plus의 `IPage`를 우리 API 응답 포맷으로 변환하는 제네릭 DTO입니다.
> ARCHITECTURE.md 8.3 페이징 응답 포맷과 일치합니다.

```java
package com.aroon.business.dto.response;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.function.Function;

@Data
@Builder
public class PageResponse<T> {

    private List<T> records;     // 현재 페이지 데이터
    private long total;          // 전체 건수
    private long size;           // 페이지 크기
    private long current;        // 현재 페이지 (1부터 시작)
    private long pages;          // 전체 페이지 수

    /**
     * MyBatis-Plus IPage → PageResponse 변환 (Entity 그대로)
     */
    public static <T> PageResponse<T> of(IPage<T> page) {
        return PageResponse.<T>builder()
                .records(page.getRecords())
                .total(page.getTotal())
                .size(page.getSize())
                .current(page.getCurrent())
                .pages(page.getPages())
                .build();
    }

    /**
     * MyBatis-Plus IPage → PageResponse 변환 (Entity → DTO 변환 포함)
     *
     * 사용 예시:
     *   PageResponse.of(userPage, this::toResponse)
     */
    public static <E, T> PageResponse<T> of(IPage<E> page, Function<E, T> converter) {
        List<T> convertedRecords = page.getRecords().stream()
                .map(converter)
                .toList();

        return PageResponse.<T>builder()
                .records(convertedRecords)
                .total(page.getTotal())
                .size(page.getSize())
                .current(page.getCurrent())
                .pages(page.getPages())
                .build();
    }
}
```

### 사용 예시 (미리보기)

```java
// Controller
@GetMapping
public ApiResponse<PageResponse<UserResponse>> getUsers(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
    return ApiResponse.success(userService.getUsers(page, size));
}

// Service
public PageResponse<UserResponse> getUsers(int page, int size) {
    Page<User> userPage = userMapper.selectPage(
            new Page<>(page, size), null
    );
    return PageResponse.of(userPage, this::toResponse);
}
```

### 응답 형태

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "records": [
      { "userSeq": 1, "username": "admin", ... },
      { "userSeq": 2, "username": "user1", ... }
    ],
    "total": 50,
    "size": 10,
    "current": 1,
    "pages": 5
  },
  "timestamp": "2026-02-13T10:00:00"
}
```

---

## Step 4-2. Translation 엔티티 + Mapper

### 4-2-A. Translation.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/entity/Translation.java`

```java
package com.aroon.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("translation")
public class Translation {

    @TableId(value = "translation_seq", type = IdType.AUTO)
    private Long translationSeq;

    private String tableName;

    private String columnName;

    private Long recordSeq;

    private String locale;

    private String value;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

---

### 4-2-B. TranslationMapper.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/mapper/TranslationMapper.java`

```java
package com.aroon.business.mapper;

import com.aroon.business.entity.Translation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TranslationMapper extends BaseMapper<Translation> {

    /**
     * 단일 번역 조회
     */
    Translation findTranslation(@Param("tableName") String tableName,
                                 @Param("columnName") String columnName,
                                 @Param("recordSeq") Long recordSeq,
                                 @Param("locale") String locale);

    /**
     * 일괄 번역 조회 (N+1 방지)
     */
    List<Translation> findTranslations(@Param("tableName") String tableName,
                                        @Param("columnName") String columnName,
                                        @Param("recordSeqs") List<Long> recordSeqs,
                                        @Param("locale") String locale);
}
```

---

### 4-2-C. TranslationMapper.xml (신규)

**파일 경로**: `src/main/resources/mapper/TranslationMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.aroon.business.mapper.TranslationMapper">

    <!-- 단일 번역 조회 -->
    <select id="findTranslation" resultType="com.aroon.business.entity.Translation">
        SELECT translation_seq, table_name, column_name, record_seq, locale, value,
               created_at, updated_at
        FROM translation
        WHERE table_name = #{tableName}
          AND column_name = #{columnName}
          AND record_seq = #{recordSeq}
          AND locale = #{locale}
    </select>

    <!-- 일괄 번역 조회 (IN 절) -->
    <select id="findTranslations" resultType="com.aroon.business.entity.Translation">
        SELECT translation_seq, table_name, column_name, record_seq, locale, value,
               created_at, updated_at
        FROM translation
        WHERE table_name = #{tableName}
          AND column_name = #{columnName}
          AND locale = #{locale}
          AND record_seq IN
        <foreach collection="recordSeqs" item="seq" open="(" separator="," close=")">
            #{seq}
        </foreach>
    </select>

</mapper>
```

### SQL 실행 예시

```
findTranslation("menu", "menu_name", 1, "en")
→ SELECT ... WHERE table_name='menu' AND column_name='menu_name' AND record_seq=1 AND locale='en'
→ 결과: { value: "System Management" }

findTranslations("menu", "menu_name", [1,2,3,4,5], "en")
→ SELECT ... WHERE ... AND record_seq IN (1,2,3,4,5) AND locale='en'
→ 결과: 5건 (각 메뉴의 영어 이름)
```

---

## Step 4-3. TranslationHelper (번역 공통 유틸리티 + 캐싱)

### 파일 경로

```
src/main/java/com/aroon/business/common/util/TranslationHelper.java
```

### 역할

동적 콘텐츠(메뉴명, 역할명, 권한명)의 번역을 처리하는 공통 유틸리티입니다.
Service 계층에서 주입받아 사용합니다.

### 핵심 설계: 캐싱

번역 데이터는 거의 변하지 않는 데이터입니다. 매 API 요청마다 DB를 조회하면 불필요한 부하가 발생하므로,
**서버 기동 시 전체 번역을 메모리에 로딩**하고, 이후에는 메모리에서만 조회합니다.

```
┌───────────────────────────────────────────────────────────────┐
│                    번역 캐싱 흐름                                │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  [서버 기동]                                                   │
│    @PostConstruct → DB에서 전체 번역 로딩 → ConcurrentHashMap  │
│                                                               │
│  [API 요청] Accept-Language: en                               │
│    1. locale이 "ko"(기본 언어)?  → 번역 불필요, DB 원본값 반환  │
│    2. locale이 "en"/"zh"?       → 캐시(메모리)에서 즉시 조회   │
│                                                               │
│  [번역 관리 API에서 등록/수정/삭제]                              │
│    → 캐시 무효화(reload) → DB에서 전체 재로딩                   │
│                                                               │
│  ⚡ DB 조회: 서버 기동 시 1회 + 번역 변경 시에만 발생           │
│  ⚡ 일반 API 요청: DB 조회 0회 (메모리에서 즉시 반환)           │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

### 캐시 키 설계

```
캐시 구조: ConcurrentHashMap<String, String>
캐시 키:   "{table_name}:{column_name}:{record_seq}:{locale}"

예시:
  "menu:menu_name:1:en"  → "System Management"
  "menu:menu_name:1:zh"  → "系统管理"
  "role:role_name:1:en"  → "Administrator"
```

### 소스 코드

```java
package com.aroon.business.common.util;

import com.aroon.business.entity.Translation;
import com.aroon.business.mapper.TranslationMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranslationHelper {

    private final TranslationMapper translationMapper;

    /**
     * 기본 언어 (DB 원본값 언어)
     * 기본 언어 요청 시 번역 조회를 하지 않고 DB 원본값을 그대로 사용합니다.
     */
    public static final String DEFAULT_LOCALE = "ko";

    /**
     * 번역 캐시
     * key: "table:column:recordSeq:locale"
     * value: 번역된 텍스트
     */
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    // ──────────────────────────────────────
    // 캐시 초기화 (서버 기동 시)
    // ──────────────────────────────────────

    /**
     * 서버 기동 시 DB의 전체 번역 데이터를 캐시에 로딩합니다.
     */
    @PostConstruct
    public void warmUp() {
        loadAllTranslations();
    }

    /**
     * DB에서 전체 번역을 조회하여 캐시에 적재합니다.
     * 번역 등록/수정/삭제 시에도 이 메서드를 호출하여 캐시를 갱신합니다.
     */
    public void reload() {
        cache.clear();
        loadAllTranslations();
        log.info("Translation cache reloaded. total entries: {}", cache.size());
    }

    private void loadAllTranslations() {
        // translation 테이블 전체 조회
        List<Translation> allTranslations = translationMapper.selectList(null);
        for (Translation t : allTranslations) {
            String key = buildKey(t.getTableName(), t.getColumnName(),
                    t.getRecordSeq(), t.getLocale());
            cache.put(key, t.getValue());
        }
        log.info("Translation cache warmed up. total entries: {}", cache.size());
    }

    // ──────────────────────────────────────
    // 번역 조회 (캐시에서)
    // ──────────────────────────────────────

    /**
     * 단일 번역 조회 (캐시에서 조회)
     *
     * @param tableName  대상 테이블명 (예: "menu")
     * @param columnName 대상 컬럼명 (예: "menu_name")
     * @param recordSeq  대상 레코드 SEQ
     * @param locale     언어 코드 (예: "en")
     * @return 번역된 값, 없으면 null (Service에서 DB 원본값 사용)
     */
    public String translate(String tableName, String columnName,
                            Long recordSeq, String locale) {
        // 기본 언어면 조회하지 않음 (DB 원본값 사용)
        if (DEFAULT_LOCALE.equals(locale)) {
            return null;
        }

        // 캐시에서 조회
        String key = buildKey(tableName, columnName, recordSeq, locale);
        return cache.get(key);
    }

    /**
     * 일괄 번역 조회 (캐시에서 조회, N+1 문제 없음)
     *
     * @param tableName  대상 테이블명
     * @param columnName 대상 컬럼명
     * @param recordSeqs 대상 레코드 SEQ 목록
     * @param locale     언어 코드
     * @return Map<recordSeq, 번역값>  (번역이 없는 recordSeq는 Map에 미포함)
     */
    public Map<Long, String> translateBatch(String tableName, String columnName,
                                             List<Long> recordSeqs, String locale) {
        // 기본 언어면 빈 Map 반환 (DB 원본값 사용)
        if (DEFAULT_LOCALE.equals(locale)) {
            return Collections.emptyMap();
        }

        if (recordSeqs == null || recordSeqs.isEmpty()) {
            return Collections.emptyMap();
        }

        // 캐시에서 일괄 조회
        Map<Long, String> resultMap = new HashMap<>();
        for (Long recordSeq : recordSeqs) {
            String key = buildKey(tableName, columnName, recordSeq, locale);
            String value = cache.get(key);
            if (value != null) {
                resultMap.put(recordSeq, value);
            }
        }

        return resultMap;
    }

    // ──────────────────────────────────────
    // 캐시 키 생성
    // ──────────────────────────────────────

    private String buildKey(String tableName, String columnName,
                            Long recordSeq, String locale) {
        return tableName + ":" + columnName + ":" + recordSeq + ":" + locale;
    }
}
```

### 기존 방식 vs 캐싱 방식 비교

```
[ 기존 방식 — 매 요청마다 DB 조회 ]

GET /api/menus (Accept-Language: en)
  1. SELECT * FROM menu                    ← DB 1회
  2. SELECT * FROM translation WHERE ...   ← DB 1회 (매번!)
  합계: 매 요청마다 DB 2회


[ 캐싱 방식 — 메모리에서 즉시 반환 ]

서버 기동 시:
  SELECT * FROM translation                ← DB 1회 (기동 시 1번만!)
  → ConcurrentHashMap에 전체 로딩

GET /api/menus (Accept-Language: ko)
  1. SELECT * FROM menu                    ← DB 1회
  2. locale이 "ko" → 번역 조회 스킵!       ← DB 0회
  합계: DB 1회

GET /api/menus (Accept-Language: en)
  1. SELECT * FROM menu                    ← DB 1회
  2. cache.get("menu:menu_name:1:en")      ← 메모리 조회 (DB 0회)
  합계: DB 1회
```

### 캐시 갱신 정책

| 이벤트 | 처리 |
|--------|------|
| 서버 기동 | `@PostConstruct` → 전체 로딩 (Warm-up) |
| 번역 등록/수정/삭제 | `translationHelper.reload()` → 전체 재로딩 |
| 일반 API 요청 | 캐시에서만 조회, DB 미접근 |

> **왜 전체 재로딩인가?**
> 번역 데이터는 전체 수백~수천 건 수준이므로 전체 재로딩 비용이 매우 낮습니다.
> 부분 갱신(evict)보다 전체 재로딩이 코드가 단순하고 데이터 일관성이 보장됩니다.
> 번역 변경은 관리자가 가끔 수행하는 작업이므로 빈도도 매우 낮습니다.

### 사용 흐름

```
Service에서 메뉴 목록 조회 시:

1. menuMapper.selectList() → 전체 메뉴 엔티티 조회 (DB 1회)
2. locale이 "ko"?
   → YES: 번역 조회 안 함. DB 원본 menu_name 그대로 사용 (DB 0회)
   → NO:  translationHelper.translateBatch("menu", "menu_name", menuSeqs, "en")
          → 캐시(메모리)에서 즉시 조회 (DB 0회)
3. 각 MenuResponse의 menuName을 번역된 값으로 교체
4. 번역이 없으면 원래 DB의 menu_name 유지
```

---

## Step 4-4. 사용자-역할 할당 API

### API 정의

```
PUT /api/users/{userSeq}/roles
권한: user:update
기능: 해당 사용자의 역할을 일괄 교체 (기존 역할 삭제 → 새 역할 삽입)
```

### 4-4-A. RoleAssignRequest.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/dto/request/RoleAssignRequest.java`

```java
package com.aroon.business.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RoleAssignRequest {

    @NotNull(message = "{validation.role.seq.required}")
    private List<Long> roleSeqs;
}
```

### 4-4-B. UserService.java — 메서드 추가

기존 인터페이스에 아래 메서드를 추가합니다.

```java
// 기존 메서드 유지 + 아래 추가

/**
 * 사용자에게 역할을 할당 (기존 역할 전체 교체)
 *
 * @param userSeq  대상 사용자 SEQ
 * @param roleSeqs 할당할 역할 SEQ 목록
 */
void assignRoles(Long userSeq, List<Long> roleSeqs);

/**
 * 사용자에게 할당된 역할 SEQ 목록 조회
 */
List<Long> getUserRoleSeqs(Long userSeq);
```

### 4-4-C. UserServiceImpl.java — 구현 추가

기존 필드에 `UserRoleMapper`를 추가하고, 메서드를 구현합니다.

**1. 필드 추가**:

```java
private final UserMapper userMapper;
private final PasswordEncoder passwordEncoder;
private final UserRoleMapper userRoleMapper;   // ← 추가
```

**2. 메서드 구현**:

```java
@Override
@Transactional
public void assignRoles(Long userSeq, List<Long> roleSeqs) {
    // 1. 사용자 존재 확인
    User user = userMapper.selectById(userSeq);
    if (user == null) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    // 2. 기존 역할 관계 전체 삭제
    userRoleMapper.delete(
            new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserSeq, userSeq)
    );

    // 3. 새 역할 관계 삽입
    if (roleSeqs != null && !roleSeqs.isEmpty()) {
        for (Long roleSeq : roleSeqs) {
            UserRole userRole = UserRole.builder()
                    .userSeq(userSeq)
                    .roleSeq(roleSeq)
                    .build();
            userRoleMapper.insert(userRole);
        }
    }
}

@Override
@Transactional(readOnly = true)
public List<Long> getUserRoleSeqs(Long userSeq) {
    List<UserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserSeq, userSeq)
    );
    return userRoles.stream().map(UserRole::getRoleSeq).toList();
}
```

> import 추가:
> ```java
> import com.aroon.business.entity.UserRole;
> import com.aroon.business.mapper.UserRoleMapper;
> ```

### 4-4-D. UserController.java — 엔드포인트 추가

기존 Controller에 아래 2개 엔드포인트를 추가합니다.

```java
import com.aroon.business.dto.request.RoleAssignRequest;

/**
 * 사용자 역할 할당
 * PUT /api/users/{userSeq}/roles
 */
@PutMapping("/{userSeq}/roles")
@PreAuthorize("hasAuthority('user:update')")
public ApiResponse<Void> assignRoles(@PathVariable Long userSeq,
                                      @Valid @RequestBody RoleAssignRequest request) {
    userService.assignRoles(userSeq, request.getRoleSeqs());
    return ApiResponse.success();
}

/**
 * 사용자 역할 조회
 * GET /api/users/{userSeq}/roles
 */
@GetMapping("/{userSeq}/roles")
@PreAuthorize("hasAuthority('user:read')")
public ApiResponse<List<Long>> getUserRoles(@PathVariable Long userSeq) {
    return ApiResponse.success(userService.getUserRoleSeqs(userSeq));
}
```

### 동작 흐름

```
PUT /api/users/1/roles
Body: { "roleSeqs": [1, 2] }

1. user_seq=1 사용자 존재 확인
2. user_role 테이블에서 user_seq=1인 행 전체 삭제
3. user_role에 (1,1), (1,2) 삽입
4. 결과: admin 사용자에게 ROLE_ADMIN + ROLE_MANAGER 할당

DELETE 후 INSERT 방식을 사용하는 이유:
- 복합 PK 테이블이라 upsert가 번거로움
- 전체 교체 방식이 가장 명확하고 안전
- 트랜잭션으로 묶여 있으므로 원자성 보장
```

---

## Step 4-5. 역할-권한 할당 API

### API 정의

```
PUT /api/roles/{roleSeq}/permissions
권한: role:update
기능: 해당 역할의 권한을 일괄 교체
```

### 4-5-A. PermissionAssignRequest.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/dto/request/PermissionAssignRequest.java`

```java
package com.aroon.business.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PermissionAssignRequest {

    @NotNull(message = "{validation.permission.seq.required}")
    private List<Long> permissionSeqs;
}
```

### 4-5-B. RoleService.java — 메서드 추가

기존 인터페이스에 아래 메서드를 추가합니다.

```java
// 기존 메서드 유지 + 아래 추가

/**
 * 역할에 권한을 할당 (기존 권한 전체 교체)
 */
void assignPermissions(Long roleSeq, List<Long> permissionSeqs);

/**
 * 역할에 할당된 권한 SEQ 목록 조회
 */
List<Long> getRolePermissionSeqs(Long roleSeq);
```

### 4-5-C. RoleServiceImpl.java — 구현 추가

**1. 필드 추가**:

```java
private final RoleMapper roleMapper;
private final RolePermissionMapper rolePermissionMapper;   // ← 추가
```

**2. 메서드 구현**:

```java
@Override
@Transactional
public void assignPermissions(Long roleSeq, List<Long> permissionSeqs) {
    // 1. 역할 존재 확인
    Role role = roleMapper.selectById(roleSeq);
    if (role == null) {
        throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
    }

    // 2. 기존 권한 관계 전체 삭제
    rolePermissionMapper.delete(
            new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleSeq, roleSeq)
    );

    // 3. 새 권한 관계 삽입
    if (permissionSeqs != null && !permissionSeqs.isEmpty()) {
        for (Long permissionSeq : permissionSeqs) {
            RolePermission rp = RolePermission.builder()
                    .roleSeq(roleSeq)
                    .permissionSeq(permissionSeq)
                    .build();
            rolePermissionMapper.insert(rp);
        }
    }
}

@Override
@Transactional(readOnly = true)
public List<Long> getRolePermissionSeqs(Long roleSeq) {
    List<RolePermission> list = rolePermissionMapper.selectList(
            new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleSeq, roleSeq)
    );
    return list.stream().map(RolePermission::getPermissionSeq).toList();
}
```

> import 추가:
> ```java
> import com.aroon.business.entity.RolePermission;
> import com.aroon.business.mapper.RolePermissionMapper;
> ```

### 4-5-D. RoleController.java — 엔드포인트 추가

```java
import com.aroon.business.dto.request.PermissionAssignRequest;

/**
 * 역할에 권한 할당
 * PUT /api/roles/{roleSeq}/permissions
 */
@PutMapping("/{roleSeq}/permissions")
@PreAuthorize("hasAuthority('role:update')")
public ApiResponse<Void> assignPermissions(@PathVariable Long roleSeq,
                                            @Valid @RequestBody PermissionAssignRequest request) {
    roleService.assignPermissions(roleSeq, request.getPermissionSeqs());
    return ApiResponse.success();
}

/**
 * 역할의 권한 조회
 * GET /api/roles/{roleSeq}/permissions
 */
@GetMapping("/{roleSeq}/permissions")
@PreAuthorize("hasAuthority('role:read')")
public ApiResponse<List<Long>> getRolePermissions(@PathVariable Long roleSeq) {
    return ApiResponse.success(roleService.getRolePermissionSeqs(roleSeq));
}
```

---

## Step 4-6. 역할-메뉴 할당 API

### API 정의

```
PUT /api/roles/{roleSeq}/menus
권한: role:update
기능: 해당 역할의 메뉴를 일괄 교체
```

### 4-6-A. MenuAssignRequest.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/dto/request/MenuAssignRequest.java`

```java
package com.aroon.business.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MenuAssignRequest {

    @NotNull(message = "{validation.menu.seq.required}")
    private List<Long> menuSeqs;
}
```

### 4-6-B. RoleService.java — 메서드 추가

```java
// 기존 메서드 유지 + 아래 추가

/**
 * 역할에 메뉴를 할당 (기존 메뉴 전체 교체)
 */
void assignMenus(Long roleSeq, List<Long> menuSeqs);

/**
 * 역할에 할당된 메뉴 SEQ 목록 조회
 */
List<Long> getRoleMenuSeqs(Long roleSeq);
```

### 4-6-C. RoleServiceImpl.java — 구현 추가

**1. 필드 추가** (기존 + 추가):

```java
private final RoleMapper roleMapper;
private final RolePermissionMapper rolePermissionMapper;
private final RoleMenuMapper roleMenuMapper;   // ← 추가
```

**2. 메서드 구현**:

```java
@Override
@Transactional
public void assignMenus(Long roleSeq, List<Long> menuSeqs) {
    // 1. 역할 존재 확인
    Role role = roleMapper.selectById(roleSeq);
    if (role == null) {
        throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
    }

    // 2. 기존 메뉴 관계 전체 삭제
    roleMenuMapper.delete(
            new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleSeq, roleSeq)
    );

    // 3. 새 메뉴 관계 삽입
    if (menuSeqs != null && !menuSeqs.isEmpty()) {
        for (Long menuSeq : menuSeqs) {
            RoleMenu rm = RoleMenu.builder()
                    .roleSeq(roleSeq)
                    .menuSeq(menuSeq)
                    .build();
            roleMenuMapper.insert(rm);
        }
    }
}

@Override
@Transactional(readOnly = true)
public List<Long> getRoleMenuSeqs(Long roleSeq) {
    List<RoleMenu> list = roleMenuMapper.selectList(
            new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleSeq, roleSeq)
    );
    return list.stream().map(RoleMenu::getMenuSeq).toList();
}
```

> import 추가:
> ```java
> import com.aroon.business.entity.RoleMenu;
> import com.aroon.business.mapper.RoleMenuMapper;
> ```

### 4-6-D. RoleController.java — 엔드포인트 추가

```java
import com.aroon.business.dto.request.MenuAssignRequest;

/**
 * 역할에 메뉴 할당
 * PUT /api/roles/{roleSeq}/menus
 */
@PutMapping("/{roleSeq}/menus")
@PreAuthorize("hasAuthority('role:update')")
public ApiResponse<Void> assignMenus(@PathVariable Long roleSeq,
                                      @Valid @RequestBody MenuAssignRequest request) {
    roleService.assignMenus(roleSeq, request.getMenuSeqs());
    return ApiResponse.success();
}

/**
 * 역할의 메뉴 조회
 * GET /api/roles/{roleSeq}/menus
 */
@GetMapping("/{roleSeq}/menus")
@PreAuthorize("hasAuthority('role:read')")
public ApiResponse<List<Long>> getRoleMenus(@PathVariable Long roleSeq) {
    return ApiResponse.success(roleService.getRoleMenuSeqs(roleSeq));
}
```

### Step 4-4 ~ 4-6 관계 할당 패턴 요약

```
3개 관계 할당 API 모두 동일한 패턴:

1. 대상 엔티티 존재 확인 (없으면 BusinessException)
2. 기존 관계 전체 DELETE (LambdaQueryWrapper)
3. 새 관계 foreach INSERT
4. @Transactional로 원자성 보장

┌──────────────────────┬──────────────────────────────┬──────────────┐
│ API                  │ 관계 테이블                    │ 매퍼          │
├──────────────────────┼──────────────────────────────┼──────────────┤
│ PUT /users/{}/roles  │ user_role (user_seq, role_seq)│ UserRoleMapper│
│ PUT /roles/{}/perms  │ role_permission               │ RolePermMapper│
│ PUT /roles/{}/menus  │ role_menu (role_seq, menu_seq)│ RoleMenuMapper│
└──────────────────────┴──────────────────────────────┴──────────────┘
```

---

## Step 4-7. 사용자 상세 조회 확장 (역할 포함)

### 역할

사용자 상세 조회 시 할당된 역할 목록을 함께 반환합니다.
기존 `UserResponse`는 목록 조회용으로 유지하고, 상세 조회용 `UserDetailResponse`를 추가합니다.

### 4-7-A. UserDetailResponse.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/dto/response/UserDetailResponse.java`

```java
package com.aroon.business.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserDetailResponse {

    private Long userSeq;
    private String username;
    private String email;
    private String nickname;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 할당된 역할 목록
     */
    private List<RoleResponse> roles;
}
```

### 4-7-B. UserMapper.java — 메서드 추가

기존 `selectPermissionsByUserSeq`에 이어 역할 조회 메서드를 추가합니다.

```java
// 기존 메서드 유지 + 아래 추가

/**
 * 사용자에 할당된 역할 목록 조회
 * 조인 경로: user_role → role
 */
List<Role> selectRolesByUserSeq(@Param("userSeq") Long userSeq);
```

> import 추가: `import com.aroon.business.entity.Role;`

### 4-7-C. UserMapper.xml — 조인 쿼리 추가

기존 `selectPermissionsByUserSeq` 아래에 추가합니다.

```xml
<!-- 사용자에 할당된 역할 목록 조회 -->
<select id="selectRolesByUserSeq" resultType="com.aroon.business.entity.Role">
    SELECT r.role_seq, r.role_code, r.role_name, r.description,
           r.status, r.created_at, r.updated_at
    FROM user_role ur
    INNER JOIN role r ON ur.role_seq = r.role_seq
    WHERE ur.user_seq = #{userSeq}
      AND r.status = 1
    ORDER BY r.role_seq
</select>
```

### 4-7-D. UserService.java — 메서드 추가

```java
// 기존 메서드 유지 + 아래 추가

/**
 * 사용자 상세 조회 (역할 포함)
 */
UserDetailResponse getUserDetail(Long userSeq);
```

### 4-7-E. UserServiceImpl.java — 구현 추가

```java
@Override
@Transactional(readOnly = true)
public UserDetailResponse getUserDetail(Long userSeq) {
    User user = userMapper.selectById(userSeq);
    if (user == null) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    // 역할 목록 조회
    List<Role> roles = userMapper.selectRolesByUserSeq(userSeq);
    List<RoleResponse> roleResponses = roles.stream()
            .map(this::toRoleResponse)
            .toList();

    return UserDetailResponse.builder()
            .userSeq(user.getUserSeq())
            .username(user.getUsername())
            .email(user.getEmail())
            .nickname(user.getNickname())
            .status(user.getStatus())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .roles(roleResponses)
            .build();
}

private RoleResponse toRoleResponse(Role role) {
    return RoleResponse.builder()
            .roleSeq(role.getRoleSeq())
            .roleCode(role.getRoleCode())
            .roleName(role.getRoleName())
            .description(role.getDescription())
            .status(role.getStatus())
            .createdAt(role.getCreatedAt())
            .updatedAt(role.getUpdatedAt())
            .build();
}
```

> import 추가:
> ```java
> import com.aroon.business.dto.response.UserDetailResponse;
> import com.aroon.business.dto.response.RoleResponse;
> import com.aroon.business.entity.Role;
> ```

### 4-7-F. UserController.java — 상세 조회 변경

기존 `getUser` 메서드의 반환 타입을 `UserDetailResponse`로 변경합니다.

```java
// 변경 전
@GetMapping("/{userSeq}")
@PreAuthorize("hasAuthority('user:read')")
public ApiResponse<UserResponse> getUser(@PathVariable Long userSeq) {
    return ApiResponse.success(userService.getUserBySeq(userSeq));
}

// 변경 후
@GetMapping("/{userSeq}")
@PreAuthorize("hasAuthority('user:read')")
public ApiResponse<UserDetailResponse> getUser(@PathVariable Long userSeq) {
    return ApiResponse.success(userService.getUserDetail(userSeq));
}
```

### 응답 예시

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "userSeq": 1,
    "username": "admin",
    "email": "admin@aroon.com",
    "nickname": "관리자",
    "status": 1,
    "createdAt": "2026-02-13T10:00:00",
    "updatedAt": "2026-02-13T10:00:00",
    "roles": [
      {
        "roleSeq": 1,
        "roleCode": "ROLE_ADMIN",
        "roleName": "관리자",
        "description": "시스템 전체 관리 권한",
        "status": 1
      }
    ]
  }
}
```

---

## Step 4-8. 역할 상세 조회 확장 (권한/메뉴 포함)

### 역할

역할 상세 조회 시 할당된 권한 목록과 메뉴 목록을 함께 반환합니다.

### 4-8-A. RoleDetailResponse.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/dto/response/RoleDetailResponse.java`

```java
package com.aroon.business.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RoleDetailResponse {

    private Long roleSeq;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 할당된 권한 목록
     */
    private List<PermissionResponse> permissions;

    /**
     * 할당된 메뉴 목록
     */
    private List<MenuResponse> menus;
}
```

### 4-8-B. RoleMapper.java — 메서드 추가

**파일 경로**: `src/main/java/com/aroon/business/mapper/RoleMapper.java`

```java
package com.aroon.business.mapper;

import com.aroon.business.entity.Menu;
import com.aroon.business.entity.Permission;
import com.aroon.business.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 역할에 할당된 권한 목록 조회
     * 조인 경로: role_permission → permission
     */
    List<Permission> selectPermissionsByRoleSeq(@Param("roleSeq") Long roleSeq);

    /**
     * 역할에 할당된 메뉴 목록 조회
     * 조인 경로: role_menu → menu
     */
    List<Menu> selectMenusByRoleSeq(@Param("roleSeq") Long roleSeq);
}
```

### 4-8-C. RoleMapper.xml (신규)

**파일 경로**: `src/main/resources/mapper/RoleMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.aroon.business.mapper.RoleMapper">

    <!-- 역할에 할당된 권한 목록 조회 -->
    <select id="selectPermissionsByRoleSeq" resultType="com.aroon.business.entity.Permission">
        SELECT p.permission_seq, p.permission_code, p.permission_name,
               p.description, p.created_at, p.updated_at
        FROM role_permission rp
        INNER JOIN permission p ON rp.permission_seq = p.permission_seq
        WHERE rp.role_seq = #{roleSeq}
        ORDER BY p.permission_seq
    </select>

    <!-- 역할에 할당된 메뉴 목록 조회 -->
    <select id="selectMenusByRoleSeq" resultType="com.aroon.business.entity.Menu">
        SELECT m.menu_seq, m.parent_seq, m.menu_code, m.menu_name, m.menu_type,
               m.path, m.icon, m.sort_order, m.status, m.created_at, m.updated_at
        FROM role_menu rm
        INNER JOIN menu m ON rm.menu_seq = m.menu_seq
        WHERE rm.role_seq = #{roleSeq}
          AND m.status = 1
        ORDER BY m.sort_order
    </select>

</mapper>
```

### 4-8-D. RoleService.java — 메서드 추가

```java
// 기존 메서드 유지 + 아래 추가

/**
 * 역할 상세 조회 (권한 + 메뉴 포함)
 */
RoleDetailResponse getRoleDetail(Long roleSeq);
```

### 4-8-E. RoleServiceImpl.java — 구현 추가

```java
@Override
@Transactional(readOnly = true)
public RoleDetailResponse getRoleDetail(Long roleSeq) {
    Role role = roleMapper.selectById(roleSeq);
    if (role == null) {
        throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
    }

    // 권한 목록 조회
    List<Permission> permissions = roleMapper.selectPermissionsByRoleSeq(roleSeq);
    List<PermissionResponse> permissionResponses = permissions.stream()
            .map(this::toPermissionResponse)
            .toList();

    // 메뉴 목록 조회
    List<Menu> menus = roleMapper.selectMenusByRoleSeq(roleSeq);
    List<MenuResponse> menuResponses = menus.stream()
            .map(this::toMenuResponse)
            .toList();

    return RoleDetailResponse.builder()
            .roleSeq(role.getRoleSeq())
            .roleCode(role.getRoleCode())
            .roleName(role.getRoleName())
            .description(role.getDescription())
            .status(role.getStatus())
            .createdAt(role.getCreatedAt())
            .updatedAt(role.getUpdatedAt())
            .permissions(permissionResponses)
            .menus(menuResponses)
            .build();
}

private PermissionResponse toPermissionResponse(Permission permission) {
    return PermissionResponse.builder()
            .permissionSeq(permission.getPermissionSeq())
            .permissionCode(permission.getPermissionCode())
            .permissionName(permission.getPermissionName())
            .description(permission.getDescription())
            .createdAt(permission.getCreatedAt())
            .updatedAt(permission.getUpdatedAt())
            .build();
}

private MenuResponse toMenuResponse(Menu menu) {
    return MenuResponse.builder()
            .menuSeq(menu.getMenuSeq())
            .parentSeq(menu.getParentSeq())
            .menuCode(menu.getMenuCode())
            .menuName(menu.getMenuName())
            .menuType(menu.getMenuType())
            .path(menu.getPath())
            .icon(menu.getIcon())
            .sortOrder(menu.getSortOrder())
            .status(menu.getStatus())
            .createdAt(menu.getCreatedAt())
            .updatedAt(menu.getUpdatedAt())
            .build();
}
```

> import 추가:
> ```java
> import com.aroon.business.dto.response.RoleDetailResponse;
> import com.aroon.business.dto.response.PermissionResponse;
> import com.aroon.business.dto.response.MenuResponse;
> import com.aroon.business.entity.Permission;
> import com.aroon.business.entity.Menu;
> ```

### 4-8-F. RoleController.java — 상세 조회 변경

```java
// 변경 전
@GetMapping("/{roleSeq}")
@PreAuthorize("hasAuthority('role:read')")
public ApiResponse<RoleResponse> getRole(@PathVariable Long roleSeq) {
    return ApiResponse.success(roleService.getRoleBySeq(roleSeq));
}

// 변경 후
@GetMapping("/{roleSeq}")
@PreAuthorize("hasAuthority('role:read')")
public ApiResponse<RoleDetailResponse> getRole(@PathVariable Long roleSeq) {
    return ApiResponse.success(roleService.getRoleDetail(roleSeq));
}
```

> import 추가: `import com.aroon.business.dto.response.RoleDetailResponse;`

### 응답 예시

```json
{
  "code": 200,
  "data": {
    "roleSeq": 1,
    "roleCode": "ROLE_ADMIN",
    "roleName": "관리자",
    "description": "시스템 전체 관리 권한",
    "status": 1,
    "permissions": [
      { "permissionSeq": 1, "permissionCode": "user:read", "permissionName": "사용자 조회" },
      { "permissionSeq": 2, "permissionCode": "user:create", "permissionName": "사용자 생성" },
      ...
    ],
    "menus": [
      { "menuSeq": 1, "menuCode": "system", "menuName": "시스템 관리", "menuType": "DIRECTORY" },
      { "menuSeq": 2, "menuCode": "system:user", "menuName": "사용자 관리", "menuType": "MENU" },
      ...
    ]
  }
}
```

---

## Step 4-9. 사용자별 메뉴 트리 조회

### API 정의

```
GET /api/menus/user
인증: 필요 (JWT)
권한: 별도 권한 불필요 (로그인 사용자 본인의 메뉴만 반환)
기능: 현재 로그인 사용자에게 할당된 역할의 메뉴만 트리로 반환
```

### 4-9-A. MenuMapper.java — 메서드 추가

```java
package com.aroon.business.mapper;

import com.aroon.business.entity.Menu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    /**
     * 사용자에게 할당된 메뉴 목록 조회
     * 조인 경로: user_role → role_menu → menu
     */
    List<Menu> selectMenusByUserSeq(@Param("userSeq") Long userSeq);
}
```

### 4-9-B. MenuMapper.xml — 조인 쿼리 추가

기존 빈 주석을 교체합니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.aroon.business.mapper.MenuMapper">

    <!--
        사용자에게 할당된 메뉴 목록 조회
        조인 경로: user_role → role_menu → menu
        DISTINCT: 사용자가 여러 역할을 가질 때 메뉴 중복 제거
    -->
    <select id="selectMenusByUserSeq" resultType="com.aroon.business.entity.Menu">
        SELECT DISTINCT m.menu_seq, m.parent_seq, m.menu_code, m.menu_name,
               m.menu_type, m.path, m.icon, m.sort_order, m.status,
               m.created_at, m.updated_at
        FROM user_role ur
        INNER JOIN role_menu rm ON ur.role_seq = rm.role_seq
        INNER JOIN menu m ON rm.menu_seq = m.menu_seq
        WHERE ur.user_seq = #{userSeq}
          AND m.status = 1
        ORDER BY m.sort_order
    </select>

</mapper>
```

### SQL 실행 흐름

```
user_seq = 1 (admin) 으로 조회하면:

user_role:    user_seq=1, role_seq=1    (admin → ROLE_ADMIN)
                    │
                    ▼
role_menu:    role_seq=1, menu_seq=1    (ROLE_ADMIN → 시스템 관리)
              role_seq=1, menu_seq=2    (ROLE_ADMIN → 사용자 관리)
              role_seq=1, menu_seq=3    (ROLE_ADMIN → 역할 관리)
              role_seq=1, menu_seq=4    (ROLE_ADMIN → 권한 관리)
              role_seq=1, menu_seq=5    (ROLE_ADMIN → 메뉴 관리)
                    │
                    ▼
menu:         5건 반환

사용자가 ROLE_USER만 가지고 있고, ROLE_USER에 메뉴 2,3만 할당되어 있다면:
→ 메뉴 2,3만 반환됨
→ 상위 메뉴(1)가 없으므로 트리 구성 시 최상위가 비어있게 됨

이를 해결하기 위해 Service에서 부모 메뉴를 자동으로 포함합니다.
```

### 4-9-C. MenuService.java — 메서드 추가

```java
// 기존 메서드 유지 + 아래 추가

/**
 * 현재 로그인 사용자의 메뉴 트리 조회
 *
 * @param userSeq 로그인 사용자 SEQ
 * @return 사용자에게 할당된 메뉴의 트리 구조
 */
List<MenuResponse> getUserMenuTree(Long userSeq);
```

### 4-9-D. MenuServiceImpl.java — 구현 추가

```java
@Override
@Transactional(readOnly = true)
public List<MenuResponse> getUserMenuTree(Long userSeq) {
    // 1. 사용자에게 할당된 메뉴 조회
    List<Menu> userMenus = menuMapper.selectMenusByUserSeq(userSeq);

    if (userMenus.isEmpty()) {
        return List.of();
    }

    // 2. 부모 메뉴 자동 포함
    //    하위 메뉴만 할당된 경우, 상위 디렉토리 메뉴도 포함시켜야 트리가 완성됨
    Set<Long> menuSeqs = userMenus.stream()
            .map(Menu::getMenuSeq)
            .collect(Collectors.toSet());

    Set<Long> parentSeqs = userMenus.stream()
            .map(Menu::getParentSeq)
            .filter(Objects::nonNull)
            .filter(seq -> !menuSeqs.contains(seq))
            .collect(Collectors.toSet());

    if (!parentSeqs.isEmpty()) {
        List<Menu> parentMenus = menuMapper.selectBatchIds(parentSeqs);
        userMenus = new ArrayList<>(userMenus);
        userMenus.addAll(parentMenus);
    }

    // 3. 트리 구성 (Phase 2의 getMenuTree()와 동일 로직)
    List<MenuResponse> allResponses = userMenus.stream()
            .map(this::toResponse)
            .toList();

    Map<Long, List<MenuResponse>> childrenMap = allResponses.stream()
            .filter(menu -> menu.getParentSeq() != null)
            .collect(Collectors.groupingBy(MenuResponse::getParentSeq));

    allResponses.forEach(menu -> {
        List<MenuResponse> children = childrenMap.get(menu.getMenuSeq());
        if (children != null && !children.isEmpty()) {
            menu.setChildren(children);
        }
    });

    return allResponses.stream()
            .filter(menu -> menu.getParentSeq() == null)
            .toList();
}
```

> import 추가:
> ```java
> import java.util.ArrayList;
> import java.util.Objects;
> import java.util.Set;
> ```

### 4-9-E. MenuController.java — 엔드포인트 추가

> **중요**: `/api/menus/user` 경로는 `/api/menus/{menuSeq}`보다 **위에** 선언해야 합니다.
> Spring은 경로를 순서대로 매칭하므로, "user"가 `{menuSeq}`로 잡히지 않도록 주의하세요.
> 기존 Phase 2에서 만든 MenuController에서 메서드 순서를 조정합니다.

```java
import com.aroon.business.security.SecurityUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * 현재 로그인 사용자의 메뉴 트리 조회
 * GET /api/menus/user
 * 별도 권한 불필요 (로그인만 하면 됨)
 */
@GetMapping("/user")
public ApiResponse<List<MenuResponse>> getUserMenuTree(
        @AuthenticationPrincipal SecurityUserDetails userDetails) {
    return ApiResponse.success(menuService.getUserMenuTree(userDetails.getUserSeq()));
}
```

**MenuController 메서드 순서 (최종)**:

```java
@GetMapping("/user")    ← 구체적 경로가 먼저!
@GetMapping             ← 메뉴 트리 (전체)
@GetMapping("/list")    ← 메뉴 리스트 (flat)
@GetMapping("/{menuSeq}") ← PathVariable은 마지막
@PostMapping
@PutMapping("/{menuSeq}")
@DeleteMapping("/{menuSeq}")
```

### 응답 예시 (ROLE_USER 사용자)

```json
{
  "code": 200,
  "data": [
    {
      "menuSeq": 1,
      "menuName": "시스템 관리",
      "menuType": "DIRECTORY",
      "children": [
        {
          "menuSeq": 2,
          "menuName": "사용자 관리",
          "menuType": "MENU",
          "path": "/system/user"
        }
      ]
    }
  ]
}
```

> ROLE_USER에 사용자 관리 메뉴만 할당되어 있다면, 상위 "시스템 관리" 디렉토리도 자동 포함되고
> 하위에는 할당된 "사용자 관리"만 표시됩니다.

---

## Step 4-10. 페이징 적용

### 변경 개요

Phase 2에서 만든 `getAllXxx()` 메서드를 페이징 방식으로 변경합니다.
기존 전체 조회도 유지하되, 목록 API의 기본은 페이징 방식으로 바꿉니다.

### 4-10-A. UserService.java — 페이징 메서드 추가

```java
// 기존 getAllUsers() 유지 + 아래 추가

/**
 * 사용자 목록 페이징 조회
 *
 * @param page 페이지 번호 (1부터 시작)
 * @param size 페이지 크기
 */
PageResponse<UserResponse> getUsers(int page, int size);
```

### 4-10-B. UserServiceImpl.java — 구현

```java
@Override
@Transactional(readOnly = true)
public PageResponse<UserResponse> getUsers(int page, int size) {
    Page<User> userPage = userMapper.selectPage(
            new Page<>(page, size), null
    );
    return PageResponse.of(userPage, this::toResponse);
}
```

> import 추가:
> ```java
> import com.aroon.business.dto.response.PageResponse;
> import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
> ```

### 4-10-C. UserController.java — 목록 API 변경

```java
// 변경 전
@GetMapping
@PreAuthorize("hasAuthority('user:read')")
public ApiResponse<List<UserResponse>> getUsers() {
    return ApiResponse.success(userService.getAllUsers());
}

// 변경 후
@GetMapping
@PreAuthorize("hasAuthority('user:read')")
public ApiResponse<PageResponse<UserResponse>> getUsers(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
    return ApiResponse.success(userService.getUsers(page, size));
}
```

### 4-10-D. RoleService / RoleServiceImpl — 동일 패턴 적용

**RoleService.java** 추가:
```java
PageResponse<RoleResponse> getRoles(int page, int size);
```

**RoleServiceImpl.java** 구현:
```java
@Override
@Transactional(readOnly = true)
public PageResponse<RoleResponse> getRoles(int page, int size) {
    Page<Role> rolePage = roleMapper.selectPage(
            new Page<>(page, size), null
    );
    return PageResponse.of(rolePage, this::toResponse);
}
```

**RoleController.java** 변경:
```java
@GetMapping
@PreAuthorize("hasAuthority('role:read')")
public ApiResponse<PageResponse<RoleResponse>> getRoles(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
    return ApiResponse.success(roleService.getRoles(page, size));
}
```

### 4-10-E. PermissionService / PermissionServiceImpl — 동일 패턴 적용

**PermissionService.java** 추가:
```java
PageResponse<PermissionResponse> getPermissions(int page, int size);
```

**PermissionServiceImpl.java** 구현:
```java
@Override
@Transactional(readOnly = true)
public PageResponse<PermissionResponse> getPermissions(int page, int size) {
    Page<Permission> permissionPage = permissionMapper.selectPage(
            new Page<>(page, size), null
    );
    return PageResponse.of(permissionPage, this::toResponse);
}
```

**PermissionController.java** 변경:
```java
@GetMapping
@PreAuthorize("hasAuthority('permission:read')")
public ApiResponse<PageResponse<PermissionResponse>> getPermissions(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
    return ApiResponse.success(permissionService.getPermissions(page, size));
}
```

### 4-10-F. 메뉴는 페이징 미적용

> 메뉴는 트리 구조이므로 페이징을 적용하지 않습니다.
> `GET /api/menus`는 트리 전체 반환, `GET /api/menus/list`는 flat 전체 반환을 유지합니다.
> 메뉴 수가 수백 건을 넘지 않는 것이 일반적이므로 전체 조회가 적합합니다.

### 페이징 동작 확인

```bash
# 기본 페이징 (1페이지, 10건)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users

# 2페이지, 5건
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/users?page=2&size=5"

# 권한 목록 페이징
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/permissions?page=1&size=5"
```

---

## Step 4-11. 동적 콘텐츠 다국어 적용

### 변경 개요

Step 4-3에서 만든 `TranslationHelper`를 메뉴/역할/권한 Service에 적용합니다.
`Accept-Language` 헤더의 locale 값을 Controller → Service로 전달합니다.

> **캐싱 & 기본 언어 스킵 동작 원리**
>
> Step 4-3에서 `TranslationHelper`는 서버 기동 시 전체 번역 데이터를 메모리에 로딩합니다.
> 따라서 아래 Service 코드에서 `translateBatch()`를 호출해도 **DB 조회가 발생하지 않습니다**.
>
> 또한, `TranslationHelper` 내부에서 `DEFAULT_LOCALE("ko")`이면 **빈 Map을 즉시 반환**하므로,
> 한국어 요청 시에는 캐시 조회조차 하지 않고 DB 원본 값을 그대로 사용합니다.
>
> ```
> 요청 흐름:
> Controller (Locale → String) → Service → TranslationHelper
>                                             ├── locale == "ko" → 즉시 빈 Map 반환 (조회 없음)
>                                             └── locale == "en"/"zh" → 캐시(ConcurrentHashMap)에서 조회
> ```

### 4-11-A. Locale 전달 방식

Controller에서 `Locale`을 파라미터로 받아 Service에 전달합니다.
Spring MVC는 `Accept-Language` 헤더를 자동으로 `Locale` 객체로 변환합니다.

```java
// Controller 메서드에 Locale 파라미터 추가
@GetMapping
public ApiResponse<List<MenuResponse>> getMenuTree(Locale locale) {
    return ApiResponse.success(menuService.getMenuTree(locale.getLanguage()));
}
```

### 4-11-B. MenuServiceImpl.java — 다국어 적용

기존 `getMenuTree()` 메서드를 수정합니다.

**1. 필드 추가**:
```java
private final MenuMapper menuMapper;
private final TranslationHelper translationHelper;   // ← 추가
```

**2. getMenuTree() 수정** — locale 파라미터 추가:

```java
// MenuService 인터페이스 수정
List<MenuResponse> getMenuTree(String locale);
List<MenuResponse> getAllMenus(String locale);

// MenuServiceImpl 구현 수정
@Override
@Transactional(readOnly = true)
public List<MenuResponse> getMenuTree(String locale) {
    // 1. 전체 메뉴 조회
    List<Menu> allMenus = menuMapper.selectList(
            new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSortOrder)
    );

    // 2. 번역 일괄 조회 (캐시에서 반환, ko이면 빈 Map 즉시 반환)
    List<Long> menuSeqs = allMenus.stream().map(Menu::getMenuSeq).toList();
    Map<Long, String> nameMap = translationHelper.translateBatch(
            "menu", "menu_name", menuSeqs, locale
    );

    // 3. Response 변환 (번역 적용)
    List<MenuResponse> allResponses = allMenus.stream()
            .map(menu -> {
                MenuResponse response = toResponse(menu);
                // 번역이 있으면 교체, 없으면 원래 DB 값(한국어) 유지
                String translatedName = nameMap.get(menu.getMenuSeq());
                if (translatedName != null) {
                    response.setMenuName(translatedName);
                }
                return response;
            })
            .toList();

    // 4. 트리 구성 (기존과 동일)
    Map<Long, List<MenuResponse>> childrenMap = allResponses.stream()
            .filter(menu -> menu.getParentSeq() != null)
            .collect(Collectors.groupingBy(MenuResponse::getParentSeq));

    allResponses.forEach(menu -> {
        List<MenuResponse> children = childrenMap.get(menu.getMenuSeq());
        if (children != null && !children.isEmpty()) {
            menu.setChildren(children);
        }
    });

    return allResponses.stream()
            .filter(menu -> menu.getParentSeq() == null)
            .toList();
}
```

> **성능 참고**: `translateBatch()` 내부 동작
> - `locale == "ko"` → `Collections.emptyMap()` 즉시 반환 (캐시 조회도 안 함)
> - `locale == "en"` / `"zh"` → `ConcurrentHashMap.get()` N번 호출 (O(1) × N, DB 접근 없음)

> import 추가:
> ```java
> import com.aroon.business.common.util.TranslationHelper;
> import java.util.Map;
> ```

**3. getUserMenuTree() 수정** — locale 파라미터 추가:

동일한 방식으로 `getUserMenuTree(Long userSeq, String locale)`로 수정하고
번역 로직을 추가합니다.

### 4-11-C. MenuController.java — locale 전달

```java
import java.util.Locale;

// 메뉴 트리 (전체)
@GetMapping
@PreAuthorize("hasAuthority('menu:read')")
public ApiResponse<List<MenuResponse>> getMenuTree(Locale locale) {
    return ApiResponse.success(menuService.getMenuTree(locale.getLanguage()));
}

// 사용자별 메뉴 트리
@GetMapping("/user")
public ApiResponse<List<MenuResponse>> getUserMenuTree(
        @AuthenticationPrincipal SecurityUserDetails userDetails,
        Locale locale) {
    return ApiResponse.success(
            menuService.getUserMenuTree(userDetails.getUserSeq(), locale.getLanguage())
    );
}
```

### 4-11-D. RoleServiceImpl / PermissionServiceImpl — 동일 패턴

역할명, 권한명에도 같은 방식으로 번역을 적용합니다.
`translateBatch()` 내부에서 기본 언어(ko)를 스킵하므로 호출하는 쪽에서는 별도 분기 없이 호출하면 됩니다.

**RoleServiceImpl 예시**:
```java
private final TranslationHelper translationHelper;

// getRoles(int page, int size, String locale) 에서:
// → ko이면 내부에서 빈 Map 반환, en/zh이면 캐시에서 조회
Map<Long, String> nameMap = translationHelper.translateBatch(
        "role", "role_name", roleSeqs, locale
);
```

**PermissionServiceImpl 예시**:
```java
private final TranslationHelper translationHelper;

// getPermissions(int page, int size, String locale) 에서:
Map<Long, String> nameMap = translationHelper.translateBatch(
        "permission", "permission_name", permSeqs, locale
);
```

> 구체적인 코드 패턴은 MenuServiceImpl과 동일하므로 생략합니다.
> Controller에서 `Locale locale` 파라미터를 추가하고, `locale.getLanguage()`를 Service에 전달하면 됩니다.

### 동작 확인

```bash
# 한국어 (기본)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/menus

# 영어
curl -H "Authorization: Bearer $TOKEN" \
     -H "Accept-Language: en" \
     http://localhost:8080/api/menus

# 중국어
curl -H "Authorization: Bearer $TOKEN" \
     -H "Accept-Language: zh" \
     http://localhost:8080/api/menus
```

영어 응답 예시:
```json
{
  "code": 200,
  "data": [
    {
      "menuSeq": 1,
      "menuName": "System Management",
      "children": [
        { "menuSeq": 2, "menuName": "User Management" },
        { "menuSeq": 3, "menuName": "Role Management" },
        { "menuSeq": 4, "menuName": "Permission Management" },
        { "menuSeq": 5, "menuName": "Menu Management" }
      ]
    }
  ]
}
```

---

## Step 4-12. 번역 관리 CRUD API (선택)

> 이 Step은 **선택 사항**입니다.
> 번역 데이터를 운영 화면에서 관리하려면 구현하고, SQL로 직접 관리해도 된다면 건너뛰어도 됩니다.

> **캐시 무효화 포인트**
>
> 번역 데이터를 등록/수정/삭제하면 Step 4-3에서 로딩한 **메모리 캐시와 DB가 불일치**합니다.
> 따라서 CUD 메서드 실행 후 반드시 `translationHelper.reload()`를 호출해서 캐시를 갱신해야 합니다.
>
> ```
> 번역 CUD 흐름:
> Controller → TranslationServiceImpl
>                ├── DB 반영 (insert / update / delete)
>                └── translationHelper.reload()  ← 캐시 전체 재로딩
> ```

### API 정의

| Method | URI | 설명 | 권한 |
|--------|-----|------|------|
| GET | /api/translations | 번역 목록 조회 (필터링) | permission:read |
| POST | /api/translations | 번역 등록 | permission:create |
| PUT | /api/translations/{translationSeq} | 번역 수정 | permission:update |
| DELETE | /api/translations/{translationSeq} | 번역 삭제 | permission:delete |

### 4-12-A. TranslationResponse.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/dto/response/TranslationResponse.java`

```java
package com.aroon.business.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TranslationResponse {

    private Long translationSeq;
    private String tableName;
    private String columnName;
    private Long recordSeq;
    private String locale;
    private String value;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 4-12-B. TranslationService.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/service/TranslationService.java`

```java
package com.aroon.business.service;

import com.aroon.business.dto.response.PageResponse;
import com.aroon.business.dto.response.TranslationResponse;
import com.aroon.business.entity.Translation;

public interface TranslationService {

    PageResponse<TranslationResponse> getTranslations(String tableName, String locale,
                                                       int page, int size);

    TranslationResponse createTranslation(Translation translation);

    TranslationResponse updateTranslation(Long translationSeq, Translation translation);

    void deleteTranslation(Long translationSeq);
}
```

### 4-12-C. TranslationServiceImpl.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/service/impl/TranslationServiceImpl.java`

```java
package com.aroon.business.service.impl;

import com.aroon.business.common.exception.BusinessException;
import com.aroon.business.common.exception.ErrorCode;
import com.aroon.business.common.util.TranslationHelper;
import com.aroon.business.dto.response.PageResponse;
import com.aroon.business.dto.response.TranslationResponse;
import com.aroon.business.entity.Translation;
import com.aroon.business.mapper.TranslationMapper;
import com.aroon.business.service.TranslationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationServiceImpl implements TranslationService {

    private final TranslationMapper translationMapper;
    private final TranslationHelper translationHelper;   // ← 캐시 무효화용

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TranslationResponse> getTranslations(String tableName, String locale,
                                                              int page, int size) {
        LambdaQueryWrapper<Translation> wrapper = new LambdaQueryWrapper<>();
        if (tableName != null && !tableName.isBlank()) {
            wrapper.eq(Translation::getTableName, tableName);
        }
        if (locale != null && !locale.isBlank()) {
            wrapper.eq(Translation::getLocale, locale);
        }
        wrapper.orderByAsc(Translation::getTableName, Translation::getColumnName,
                Translation::getRecordSeq);

        Page<Translation> translationPage = translationMapper.selectPage(
                new Page<>(page, size), wrapper
        );
        return PageResponse.of(translationPage, this::toResponse);
    }

    @Override
    @Transactional
    public TranslationResponse createTranslation(Translation translation) {
        translationMapper.insert(translation);
        translationHelper.reload();   // ← 캐시 무효화
        return toResponse(translation);
    }

    @Override
    @Transactional
    public TranslationResponse updateTranslation(Long translationSeq, Translation translation) {
        Translation existing = translationMapper.selectById(translationSeq);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        if (translation.getValue() != null) {
            existing.setValue(translation.getValue());
        }

        translationMapper.updateById(existing);
        translationHelper.reload();   // ← 캐시 무효화
        return toResponse(translationMapper.selectById(translationSeq));
    }

    @Override
    @Transactional
    public void deleteTranslation(Long translationSeq) {
        Translation existing = translationMapper.selectById(translationSeq);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        translationMapper.deleteById(translationSeq);
        translationHelper.reload();   // ← 캐시 무효화
    }

    private TranslationResponse toResponse(Translation t) {
        return TranslationResponse.builder()
                .translationSeq(t.getTranslationSeq())
                .tableName(t.getTableName())
                .columnName(t.getColumnName())
                .recordSeq(t.getRecordSeq())
                .locale(t.getLocale())
                .value(t.getValue())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
```

### 4-12-D. TranslationController.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/controller/TranslationController.java`

```java
package com.aroon.business.controller;

import com.aroon.business.common.response.ApiResponse;
import com.aroon.business.dto.response.PageResponse;
import com.aroon.business.dto.response.TranslationResponse;
import com.aroon.business.entity.Translation;
import com.aroon.business.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/translations")
@RequiredArgsConstructor
public class TranslationController {

    private final TranslationService translationService;

    @GetMapping
    @PreAuthorize("hasAuthority('permission:read')")
    public ApiResponse<PageResponse<TranslationResponse>> getTranslations(
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String locale,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(translationService.getTranslations(tableName, locale, page, size));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('permission:create')")
    public ApiResponse<TranslationResponse> createTranslation(@RequestBody Translation translation) {
        return ApiResponse.success(translationService.createTranslation(translation));
    }

    @PutMapping("/{translationSeq}")
    @PreAuthorize("hasAuthority('permission:update')")
    public ApiResponse<TranslationResponse> updateTranslation(
            @PathVariable Long translationSeq,
            @RequestBody Translation translation) {
        return ApiResponse.success(translationService.updateTranslation(translationSeq, translation));
    }

    @DeleteMapping("/{translationSeq}")
    @PreAuthorize("hasAuthority('permission:delete')")
    public ApiResponse<Void> deleteTranslation(@PathVariable Long translationSeq) {
        translationService.deleteTranslation(translationSeq);
        return ApiResponse.success();
    }
}
```

---

## i18n messages 추가

Step 4-4 ~ 4-6에서 사용한 유효성 검증 메시지를 i18n 파일에 추가합니다.

### messages.properties (한국어)

```properties
# Phase 4 추가
validation.role.seq.required=역할 SEQ 목록은 필수입니다
validation.permission.seq.required=권한 SEQ 목록은 필수입니다
validation.menu.seq.required=메뉴 SEQ 목록은 필수입니다
```

### messages_en.properties (영어)

```properties
# Phase 4 추가
validation.role.seq.required=Role SEQ list is required
validation.permission.seq.required=Permission SEQ list is required
validation.menu.seq.required=Menu SEQ list is required
```

### messages_zh.properties (중국어)

```properties
# Phase 4 추가
validation.role.seq.required=角色SEQ列表为必填项
validation.permission.seq.required=权限SEQ列表为必填项
validation.menu.seq.required=菜单SEQ列表为必填项
```

---

## API 테스트 가이드

### 1. 사용자-역할 할당

```bash
# admin 사용자에게 ROLE_ADMIN + ROLE_MANAGER 할당
curl -X PUT -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  http://localhost:8080/api/users/1/roles \
  -d '{"roleSeqs":[1,2]}'

# admin 사용자의 역할 조회
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/users/1/roles
```

### 2. 역할-권한 할당

```bash
# ROLE_USER(roleSeq=3)에 user:read, role:read 권한만 할당
curl -X PUT -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  http://localhost:8080/api/roles/3/permissions \
  -d '{"permissionSeqs":[1,5]}'

# 역할의 권한 조회
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/roles/3/permissions
```

### 3. 역할-메뉴 할당

```bash
# ROLE_USER에 사용자 관리 메뉴만 할당
curl -X PUT -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  http://localhost:8080/api/roles/3/menus \
  -d '{"menuSeqs":[1,2]}'
```

### 4. 사용자 상세 조회 (역할 포함)

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/users/1
```

### 5. 역할 상세 조회 (권한 + 메뉴 포함)

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/roles/1
```

### 6. 사용자별 메뉴 트리

```bash
# 로그인 사용자의 메뉴만 조회
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/menus/user
```

### 7. 페이징 조회

```bash
# 사용자 목록 (2페이지, 5건)
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/users?page=2&size=5"

# 역할 목록 (1페이지, 10건)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/roles
```

### 8. 다국어 동적 콘텐츠

```bash
# 영어 메뉴 트리
curl -H "Authorization: Bearer $TOKEN" \
     -H "Accept-Language: en" \
     http://localhost:8080/api/menus

# 중국어 메뉴 트리
curl -H "Authorization: Bearer $TOKEN" \
     -H "Accept-Language: zh" \
     http://localhost:8080/api/menus
```

### 9. 번역 관리 (선택)

```bash
# 번역 목록 조회 (menu 테이블, 영어)
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/translations?tableName=menu&locale=en"

# 번역 등록
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  http://localhost:8080/api/translations \
  -d '{"tableName":"menu","columnName":"menu_name","recordSeq":1,"locale":"ja","value":"システム管理"}'
```

---

## 체크리스트

### 파일 생성/수정 체크

| # | 파일 | 상태 | 확인 |
|---|------|------|------|
| 1 | `dto/response/PageResponse.java` | 신규 | ☐ |
| 2 | `entity/Translation.java` | 신규 | ☐ |
| 3 | `mapper/TranslationMapper.java` | 신규 | ☐ |
| 4 | `resources/mapper/TranslationMapper.xml` | 신규 | ☐ |
| 5 | `common/util/TranslationHelper.java` | 신규 | ☐ |
| 6 | `dto/request/RoleAssignRequest.java` | 신규 | ☐ |
| 7 | `dto/request/PermissionAssignRequest.java` | 신규 | ☐ |
| 8 | `dto/request/MenuAssignRequest.java` | 신규 | ☐ |
| 9 | `service/UserService.java` — assignRoles, getUserDetail, getUsers 추가 | 수정 | ☐ |
| 10 | `service/impl/UserServiceImpl.java` — 구현 추가 | 수정 | ☐ |
| 11 | `controller/UserController.java` — 역할 할당, 상세 조회, 페이징 | 수정 | ☐ |
| 12 | `service/RoleService.java` — assign, getRoleDetail, getRoles 추가 | 수정 | ☐ |
| 13 | `service/impl/RoleServiceImpl.java` — 구현 추가 | 수정 | ☐ |
| 14 | `controller/RoleController.java` — 할당, 상세 조회, 페이징 | 수정 | ☐ |
| 15 | `service/PermissionService.java` — getPermissions(page) 추가 | 수정 | ☐ |
| 16 | `service/impl/PermissionServiceImpl.java` — 페이징 구현 | 수정 | ☐ |
| 17 | `controller/PermissionController.java` — 페이징 | 수정 | ☐ |
| 18 | `service/MenuService.java` — getUserMenuTree, locale 파라미터 추가 | 수정 | ☐ |
| 19 | `service/impl/MenuServiceImpl.java` — 사용자 메뉴, 다국어 | 수정 | ☐ |
| 20 | `controller/MenuController.java` — 사용자 메뉴, locale | 수정 | ☐ |
| 21 | `dto/response/UserDetailResponse.java` | 신규 | ☐ |
| 22 | `dto/response/RoleDetailResponse.java` | 신규 | ☐ |
| 23 | `mapper/UserMapper.java` — selectRolesByUserSeq 추가 | 수정 | ☐ |
| 24 | `resources/mapper/UserMapper.xml` — 역할 조인 쿼리 추가 | 수정 | ☐ |
| 25 | `mapper/RoleMapper.java` — selectPermissions/Menus 추가 | 수정 | ☐ |
| 26 | `resources/mapper/RoleMapper.xml` | 신규 | ☐ |
| 27 | `mapper/MenuMapper.java` — selectMenusByUserSeq 추가 | 수정 | ☐ |
| 28 | `resources/mapper/MenuMapper.xml` — 사용자 메뉴 조인 | 수정 | ☐ |
| 29 | `i18n/messages*.properties` — Phase 4 메시지 추가 | 수정 | ☐ |
| 30 | (선택) `dto/response/TranslationResponse.java` | 신규 | ☐ |
| 31 | (선택) `service/TranslationService.java` | 신규 | ☐ |
| 32 | (선택) `service/impl/TranslationServiceImpl.java` | 신규 | ☐ |
| 33 | (선택) `controller/TranslationController.java` | 신규 | ☐ |

### 빌드 및 기동 확인

```bash
# 컴파일 확인
./mvnw compile -DskipTests

# 서버 기동
./mvnw spring-boot:run
```

### 동작 확인 시나리오

```
1. 사용자-역할 할당 (PUT)         → 200 성공               ☐
2. 사용자 역할 조회 (GET)          → 할당된 roleSeqs 반환    ☐
3. 역할-권한 할당 (PUT)           → 200 성공               ☐
4. 역할-메뉴 할당 (PUT)           → 200 성공               ☐
5. 사용자 상세 조회               → roles 포함 응답          ☐
6. 역할 상세 조회                 → permissions + menus 포함 ☐
7. 사용자별 메뉴 트리              → 할당된 메뉴만 트리로 반환 ☐
8. 페이징 목록 조회               → records/total/pages 응답 ☐
9. 다국어 메뉴 조회 (en)          → 영어 메뉴명 반환         ☐
10. 다국어 메뉴 조회 (zh)         → 중국어 메뉴명 반환       ☐
```

---

## 전체 Phase 완료 요약

### Phase 1~4 완료 후 전체 API 목록

| Method | URI | 설명 | Phase |
|--------|-----|------|:-----:|
| POST | /api/auth/login | 로그인 | 3 |
| POST | /api/auth/refresh | 토큰 갱신 | 3 |
| GET | /api/auth/me | 내 정보 | 3 |
| GET | /api/users | 사용자 목록 (페이징) | 2+4 |
| GET | /api/users/{userSeq} | 사용자 상세 (역할 포함) | 2+4 |
| POST | /api/users | 사용자 생성 | 2 |
| PUT | /api/users/{userSeq} | 사용자 수정 | 2 |
| DELETE | /api/users/{userSeq} | 사용자 삭제 | 2 |
| PUT | /api/users/{userSeq}/roles | 사용자 역할 할당 | 4 |
| GET | /api/users/{userSeq}/roles | 사용자 역할 조회 | 4 |
| GET | /api/roles | 역할 목록 (페이징) | 2+4 |
| GET | /api/roles/{roleSeq} | 역할 상세 (권한+메뉴 포함) | 2+4 |
| POST | /api/roles | 역할 생성 | 2 |
| PUT | /api/roles/{roleSeq} | 역할 수정 | 2 |
| DELETE | /api/roles/{roleSeq} | 역할 삭제 | 2 |
| PUT | /api/roles/{roleSeq}/permissions | 역할 권한 할당 | 4 |
| GET | /api/roles/{roleSeq}/permissions | 역할 권한 조회 | 4 |
| PUT | /api/roles/{roleSeq}/menus | 역할 메뉴 할당 | 4 |
| GET | /api/roles/{roleSeq}/menus | 역할 메뉴 조회 | 4 |
| GET | /api/permissions | 권한 목록 (페이징) | 2+4 |
| GET | /api/permissions/{permissionSeq} | 권한 상세 | 2 |
| POST | /api/permissions | 권한 생성 | 2 |
| PUT | /api/permissions/{permissionSeq} | 권한 수정 | 2 |
| DELETE | /api/permissions/{permissionSeq} | 권한 삭제 | 2 |
| GET | /api/menus | 메뉴 트리 (전체, 다국어) | 2+4 |
| GET | /api/menus/user | 사용자별 메뉴 트리 (다국어) | 4 |
| GET | /api/menus/list | 메뉴 리스트 (flat) | 2 |
| GET | /api/menus/{menuSeq} | 메뉴 상세 | 2 |
| POST | /api/menus | 메뉴 생성 | 2 |
| PUT | /api/menus/{menuSeq} | 메뉴 수정 | 2 |
| DELETE | /api/menus/{menuSeq} | 메뉴 삭제 | 2 |
| GET | /api/translations | 번역 목록 (선택) | 4 |
| POST | /api/translations | 번역 등록 (선택) | 4 |
| PUT | /api/translations/{translationSeq} | 번역 수정 (선택) | 4 |
| DELETE | /api/translations/{translationSeq} | 번역 삭제 (선택) | 4 |

### 아키텍쳐 완성도

```
Phase 1  ✅  기반 구축 (ApiResponse, ErrorCode, Exception, Config, i18n)
Phase 2  ✅  엔티티 & CRUD (7 Entity, 7 Mapper, 12 DTO, 4 Module CRUD)
Phase 3  ✅  인증/인가 (JWT, Security, Auth API, @PreAuthorize)
Phase 4  ✅  관계 매핑 & 고급 기능 (할당 API, 상세 조회, 페이징, 다국어)

→ RBAC 기반 권한 관리 시스템 완성!
```
