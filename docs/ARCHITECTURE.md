# Aroon Backend Architecture Document

## 1. 개요

Spring Boot + MyBatis-Plus + Spring Security 기반의 RBAC(Role-Based Access Control) 권한 관리 시스템

### 1.1 기술 스택
| 구분 | 기술 |
|------|------|
| Framework | Spring Boot 3.5.10 |
| Security | Spring Security 6.x + JWT |
| ORM | MyBatis-Plus 3.5.5 |
| Database | MySQL 8.x |
| Java | JDK 17 |
| Build | Maven |

---

## 2. RBAC 모델 설계

### 2.1 RBAC 구조도

```
┌─────────┐       ┌─────────┐       ┌─────────────┐
│  User   │──M:N──│  Role   │──M:N──│ Permission  │
└─────────┘       └─────────┘       └─────────────┘
                       │
                       │ M:N
                       ▼
                  ┌─────────┐
                  │  Menu   │
                  └─────────┘
```

### 2.2 관계 설명
- **User ↔ Role**: 한 사용자는 여러 역할을 가질 수 있음 (다대다)
- **Role ↔ Permission**: 한 역할은 여러 권한을 가질 수 있음 (다대다)
- **Role ↔ Menu**: 한 역할은 여러 메뉴에 접근할 수 있음 (다대다)

---

## 3. 데이터베이스 설계

### 3.1 ERD

```
┌─────────────────────────┐
│         user            │
├─────────────────────────┤
│ user_seq (PK)           │
│ username                │
│ password                │
│ email                   │
│ nickname                │
│ status                  │
│ created_at              │
│ updated_at              │
└───────────┬─────────────┘
            │
            │ M:N
            ▼
┌─────────────────────────┐      ┌─────────────────────────┐
│      user_role          │      │         role            │
├─────────────────────────┤      ├─────────────────────────┤
│ user_seq (FK)           │──────│ role_seq (PK)           │
│ role_seq (FK)           │      │ role_code               │
└─────────────────────────┘      │ role_name               │
                                 │ description             │
                                 │ status                  │
                                 │ created_at              │
                                 │ updated_at              │
                                 └───────────┬─────────────┘
                                             │
              ┌──────────────────────────────┼──────────────────────────────┐
              │                              │                              │
              ▼                              ▼                              ▼
┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────────┐
│    role_permission      │  │      role_menu          │  │      permission         │
├─────────────────────────┤  ├─────────────────────────┤  ├─────────────────────────┤
│ role_seq (FK)           │  │ role_seq (FK)           │  │ permission_seq (PK)     │
│ permission_seq (FK)     │  │ menu_seq (FK)           │  │ permission_code         │
└─────────────────────────┘  └─────────────────────────┘  │ permission_name         │
                                                          │ description             │
                                                          │ created_at              │
                                                          │ updated_at              │
                                                          └─────────────────────────┘

┌─────────────────────────┐
│         menu            │
├─────────────────────────┤
│ menu_seq (PK)           │
│ parent_seq (FK)         │  ← 자기참조 (트리구조)
│ menu_code               │
│ menu_name               │
│ menu_type               │  ← DIRECTORY / MENU / BUTTON
│ path                    │
│ icon                    │
│ sort_order              │
│ status                  │
│ created_at              │
│ updated_at              │
└─────────────────────────┘
```

### 3.2 테이블 상세 정의

#### 3.2.1 user (사용자)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| user_seq | BIGINT | PK, AUTO_INCREMENT |
| username | VARCHAR(50) | 로그인 ID, UNIQUE |
| password | VARCHAR(255) | BCrypt 암호화 |
| email | VARCHAR(100) | 이메일 |
| nickname | VARCHAR(50) | 표시 이름 |
| status | TINYINT | 0:비활성, 1:활성 |
| created_at | DATETIME | 생성일시 |
| updated_at | DATETIME | 수정일시 |

#### 3.2.2 role (역할)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| role_seq | BIGINT | PK, AUTO_INCREMENT |
| role_code | VARCHAR(50) | 역할 코드 (ROLE_ADMIN) |
| role_name | VARCHAR(100) | 역할 이름 |
| description | VARCHAR(255) | 설명 |
| status | TINYINT | 0:비활성, 1:활성 |
| created_at | DATETIME | 생성일시 |
| updated_at | DATETIME | 수정일시 |

#### 3.2.3 permission (권한)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| permission_seq | BIGINT | PK, AUTO_INCREMENT |
| permission_code | VARCHAR(100) | 권한 코드 (user:read) |
| permission_name | VARCHAR(100) | 권한 이름 |
| description | VARCHAR(255) | 설명 |
| created_at | DATETIME | 생성일시 |
| updated_at | DATETIME | 수정일시 |

#### 3.2.4 menu (메뉴)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| menu_seq | BIGINT | PK, AUTO_INCREMENT |
| parent_seq | BIGINT | 상위 메뉴 SEQ (NULL=최상위) |
| menu_code | VARCHAR(50) | 메뉴 코드 |
| menu_name | VARCHAR(100) | 메뉴 이름 |
| menu_type | VARCHAR(20) | DIRECTORY/MENU/BUTTON |
| path | VARCHAR(255) | 라우트 경로 |
| icon | VARCHAR(50) | 아이콘 |
| sort_order | INT | 정렬 순서 |
| status | TINYINT | 0:비활성, 1:활성 |
| created_at | DATETIME | 생성일시 |
| updated_at | DATETIME | 수정일시 |

#### 3.2.5 관계 테이블
```sql
-- user_role
user_seq BIGINT (FK → user.user_seq)
role_seq BIGINT (FK → role.role_seq)
PRIMARY KEY (user_seq, role_seq)

-- role_permission
role_seq BIGINT (FK → role.role_seq)
permission_seq BIGINT (FK → permission.permission_seq)
PRIMARY KEY (role_seq, permission_seq)

-- role_menu
role_seq BIGINT (FK → role.role_seq)
menu_seq BIGINT (FK → menu.menu_seq)
PRIMARY KEY (role_seq, menu_seq)
```

---

## 4. 패키지 구조

```
com.aroon.business
├── AroonApplication.java
├── common/                          # 공통 모듈
│   ├── config/                      # 설정
│   │   ├── SecurityConfig.java
│   │   ├── MybatisPlusConfig.java
│   │   └── WebConfig.java
│   ├── exception/                   # 예외 처리
│   │   ├── GlobalExceptionHandler.java
│   │   ├── BusinessException.java
│   │   └── ErrorCode.java
│   ├── response/                    # 응답 포맷
│   │   └── ApiResponse.java
│   └── util/                        # 유틸리티
│       └── JwtUtil.java
│
├── security/                        # Spring Security
│   ├── jwt/
│   │   ├── JwtAuthenticationFilter.java
│   │   └── JwtTokenProvider.java
│   ├── UserDetailsServiceImpl.java
│   └── SecurityUserDetails.java
│
├── controller/                      # 컨트롤러
│   ├── AuthController.java
│   ├── UserController.java
│   ├── RoleController.java
│   ├── PermissionController.java
│   └── MenuController.java
│
├── dto/                             # DTO
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── UserCreateRequest.java
│   │   ├── UserUpdateRequest.java
│   │   └── ...
│   └── response/
│       ├── LoginResponse.java
│       ├── UserResponse.java
│       └── ...
│
├── entity/                          # 엔티티
│   ├── User.java
│   ├── Role.java
│   ├── Permission.java
│   ├── Menu.java
│   ├── UserRole.java
│   ├── RolePermission.java
│   └── RoleMenu.java
│
├── mapper/                          # 매퍼
│   ├── UserMapper.java
│   ├── RoleMapper.java
│   ├── PermissionMapper.java
│   ├── MenuMapper.java
│   ├── UserRoleMapper.java
│   ├── RolePermissionMapper.java
│   └── RoleMenuMapper.java
│
└── service/                         # 서비스
    ├── AuthService.java
    ├── UserService.java
    ├── RoleService.java
    ├── PermissionService.java
    ├── MenuService.java
    └── impl/
        ├── AuthServiceImpl.java
        ├── UserServiceImpl.java
        ├── RoleServiceImpl.java
        ├── PermissionServiceImpl.java
        └── MenuServiceImpl.java
```

---

## 5. API 설계

### 5.1 인증 API

| Method | URI | 설명 | 인증 |
|--------|-----|------|------|
| POST | /api/auth/login | 로그인 | X |
| POST | /api/auth/logout | 로그아웃 | O |
| POST | /api/auth/refresh | 토큰 갱신 | O |
| GET | /api/auth/me | 현재 사용자 정보 | O |

### 5.2 사용자 API

| Method | URI | 설명 | 권한 |
|--------|-----|------|------|
| GET | /api/users | 사용자 목록 조회 | user:read |
| GET | /api/users/{userSeq} | 사용자 상세 조회 | user:read |
| POST | /api/users | 사용자 생성 | user:create |
| PUT | /api/users/{userSeq} | 사용자 수정 | user:update |
| DELETE | /api/users/{userSeq} | 사용자 삭제 | user:delete |
| PUT | /api/users/{userSeq}/roles | 사용자 역할 할당 | user:update |

### 5.3 역할 API

| Method | URI | 설명 | 권한 |
|--------|-----|------|------|
| GET | /api/roles | 역할 목록 조회 | role:read |
| GET | /api/roles/{roleSeq} | 역할 상세 조회 | role:read |
| POST | /api/roles | 역할 생성 | role:create |
| PUT | /api/roles/{roleSeq} | 역할 수정 | role:update |
| DELETE | /api/roles/{roleSeq} | 역할 삭제 | role:delete |
| PUT | /api/roles/{roleSeq}/permissions | 역할에 권한 할당 | role:update |
| PUT | /api/roles/{roleSeq}/menus | 역할에 메뉴 할당 | role:update |

### 5.4 권한 API

| Method | URI | 설명 | 권한 |
|--------|-----|------|------|
| GET | /api/permissions | 권한 목록 조회 | permission:read |
| GET | /api/permissions/{permissionSeq} | 권한 상세 조회 | permission:read |
| POST | /api/permissions | 권한 생성 | permission:create |
| PUT | /api/permissions/{permissionSeq} | 권한 수정 | permission:update |
| DELETE | /api/permissions/{permissionSeq} | 권한 삭제 | permission:delete |

### 5.5 메뉴 API

| Method | URI | 설명 | 권한 |
|--------|-----|------|------|
| GET | /api/menus | 메뉴 목록 조회 (트리) | menu:read |
| GET | /api/menus/{menuSeq} | 메뉴 상세 조회 | menu:read |
| POST | /api/menus | 메뉴 생성 | menu:create |
| PUT | /api/menus/{menuSeq} | 메뉴 수정 | menu:update |
| DELETE | /api/menus/{menuSeq} | 메뉴 삭제 | menu:delete |
| GET | /api/menus/user | 현재 사용자 메뉴 조회 | - |

---

## 6. 인증/인가 흐름

### 6.1 로그인 흐름

```
┌────────┐     ┌────────────┐     ┌────────────┐     ┌────────┐
│ Client │────▶│ Controller │────▶│AuthService │────▶│   DB   │
└────────┘     └────────────┘     └────────────┘     └────────┘
    │                │                   │                │
    │  POST /login   │                   │                │
    │  {username,    │                   │                │
    │   password}    │                   │                │
    │───────────────▶│   authenticate   │                │
    │                │──────────────────▶│  findByUsername│
    │                │                   │───────────────▶│
    │                │                   │◀───────────────│
    │                │                   │                │
    │                │                   │ verify password│
    │                │                   │ generate JWT   │
    │                │◀──────────────────│                │
    │  {accessToken, │                   │                │
    │   refreshToken}│                   │                │
    │◀───────────────│                   │                │
```

### 6.2 API 요청 인가 흐름

```
┌────────┐     ┌───────────┐     ┌────────────┐     ┌────────────┐
│ Client │────▶│JwtFilter  │────▶│ Security   │────▶│ Controller │
└────────┘     └───────────┘     └────────────┘     └────────────┘
    │                │                  │                  │
    │ GET /api/users │                  │                  │
    │ Authorization: │                  │                  │
    │ Bearer {token} │                  │                  │
    │───────────────▶│                  │                  │
    │                │ validate token   │                  │
    │                │ extract claims   │                  │
    │                │ load permissions │                  │
    │                │─────────────────▶│                  │
    │                │                  │ @PreAuthorize   │
    │                │                  │ check permission│
    │                │                  │─────────────────▶│
    │                │                  │                  │ process
    │◀─────────────────────────────────────────────────────│
```

---

## 7. 보안 설정

### 7.1 SecurityConfig 핵심 설정

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // 인증 제외 경로
    private static final String[] WHITE_LIST = {
        "/api/auth/login",
        "/api/auth/refresh",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    };

    // JWT 필터 적용
    // CSRF 비활성화 (REST API)
    // CORS 설정
    // 세션 STATELESS
}
```

### 7.2 권한 검증 방식

```java
// 메서드 레벨 권한 검증
@PreAuthorize("hasAuthority('user:read')")
public List<User> getUsers() { ... }

@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long userSeq) { ... }

// 커스텀 권한 표현식
@PreAuthorize("@permissionChecker.hasPermission('user:delete')")
public void deleteUser(Long userSeq) { ... }
```

---

## 8. 응답 포맷

### 8.1 성공 응답

```json
{
    "code": 200,
    "message": "Success",
    "data": { ... },
    "timestamp": "2026-02-06T18:00:00"
}
```

### 8.2 에러 응답

```json
{
    "code": 401,
    "message": "Invalid credentials",
    "data": null,
    "timestamp": "2026-02-06T18:00:00"
}
```

### 8.3 페이징 응답

```json
{
    "code": 200,
    "message": "Success",
    "data": {
        "records": [ ... ],
        "total": 100,
        "size": 10,
        "current": 1,
        "pages": 10
    },
    "timestamp": "2026-02-06T18:00:00"
}
```

---

## 9. 추가 의존성

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

## 10. 구현 순서

### Phase 1: 기반 구축
1. 의존성 추가 (Security, JWT, Validation)
2. 공통 모듈 구현 (ApiResponse, Exception, Config)
3. 데이터베이스 스키마 생성

### Phase 2: 엔티티 및 CRUD
4. Entity 클래스 생성 (User, Role, Permission, Menu)
5. Mapper 인터페이스 생성
6. Service 구현
7. 기본 CRUD API 구현

### Phase 3: 인증/인가
8. JWT 유틸리티 구현
9. Spring Security 설정
10. 로그인/로그아웃 API 구현
11. 권한 검증 로직 구현

### Phase 4: 관계 및 고급 기능
12. 역할-권한 할당 API
13. 역할-메뉴 할당 API
14. 사용자별 메뉴 조회 API
15. 권한 캐싱 (선택)

---

## 11. 기본 역할 및 권한

### 11.1 기본 역할
| 코드 | 이름 | 설명 |
|------|------|------|
| ROLE_ADMIN | 관리자 | 모든 권한 |
| ROLE_MANAGER | 매니저 | 사용자 관리 제외 전체 |
| ROLE_USER | 일반 사용자 | 기본 조회 권한 |

### 11.2 기본 권한
| 코드 | 설명 |
|------|------|
| user:read | 사용자 조회 |
| user:create | 사용자 생성 |
| user:update | 사용자 수정 |
| user:delete | 사용자 삭제 |
| role:read | 역할 조회 |
| role:create | 역할 생성 |
| role:update | 역할 수정 |
| role:delete | 역할 삭제 |
| permission:read | 권한 조회 |
| permission:create | 권한 생성 |
| permission:update | 권한 수정 |
| permission:delete | 권한 삭제 |
| menu:read | 메뉴 조회 |
| menu:create | 메뉴 생성 |
| menu:update | 메뉴 수정 |
| menu:delete | 메뉴 삭제 |
