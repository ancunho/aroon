# Phase 2: 엔티티 & CRUD 상세 가이드

## 개요

Phase 1에서 구축한 기반(ApiResponse, ErrorCode, BusinessException, GlobalExceptionHandler) 위에
실제 RBAC 도메인의 **Entity → Mapper → DTO → Service → Controller** 전체 CRUD를 구현합니다.

### 전제 조건
- Phase 1의 모든 Step이 완료되어 있어야 합니다
- DB에 `schema.sql`이 실행되어 7개 RBAC 테이블 + translation 테이블이 존재해야 합니다

### Phase 2 작업 목록

| 순서 | 작업 | 생성/수정 파일 수 | 설명 |
|:----:|------|:-----------------:|------|
| 2-1 | Entity 생성/수정 | 7개 | User 수정 + 6개 신규 |
| 2-2 | Mapper 인터페이스 | 7개 | UserMapper 유지 + 6개 신규 |
| 2-3 | DTO 생성 | 12개 | Request 6개 + Response 4개 + 페이징 2개 |
| 2-4 | User 모듈 CRUD | 3개 | Service + ServiceImpl + Controller |
| 2-5 | Role 모듈 CRUD | 3개 | Service + ServiceImpl + Controller |
| 2-6 | Permission 모듈 CRUD | 3개 | Service + ServiceImpl + Controller |
| 2-7 | Menu 모듈 CRUD | 3개 | Service + ServiceImpl + Controller (트리 포함) |
| 2-8 | XML 매퍼 | 2개 | 조인 쿼리가 필요한 매퍼 |

### Phase 2 완료 후 디렉토리 구조 (신규/수정 파일만 표시)

```
src/main/java/com/aroon/business/
│
├── controller/                                ← 신규 패키지
│   ├── UserController.java
│   ├── RoleController.java
│   ├── PermissionController.java
│   └── MenuController.java
│
├── dto/                                       ← 신규 패키지
│   ├── request/
│   │   ├── UserCreateRequest.java
│   │   ├── UserUpdateRequest.java
│   │   ├── RoleCreateRequest.java
│   │   ├── RoleUpdateRequest.java
│   │   ├── MenuCreateRequest.java
│   │   └── MenuUpdateRequest.java
│   └── response/
│       ├── UserResponse.java
│       ├── RoleResponse.java
│       ├── PermissionResponse.java
│       └── MenuResponse.java
│
├── entity/                                    ← 수정 + 신규
│   ├── User.java                              (수정)
│   ├── Role.java                              (신규)
│   ├── Permission.java                        (신규)
│   ├── Menu.java                              (신규)
│   ├── UserRole.java                          (신규)
│   ├── RolePermission.java                    (신규)
│   └── RoleMenu.java                          (신규)
│
├── mapper/                                    ← 유지 + 신규
│   ├── UserMapper.java                        (유지)
│   ├── RoleMapper.java                        (신규)
│   ├── PermissionMapper.java                  (신규)
│   ├── MenuMapper.java                        (신규)
│   ├── UserRoleMapper.java                    (신규)
│   ├── RolePermissionMapper.java              (신규)
│   └── RoleMenuMapper.java                    (신규)
│
├── service/                                   ← 수정 + 신규
│   ├── UserService.java                       (수정)
│   ├── RoleService.java                       (신규)
│   ├── PermissionService.java                 (신규)
│   ├── MenuService.java                       (신규)
│   └── impl/
│       ├── UserServiceImpl.java               (수정)
│       ├── RoleServiceImpl.java               (신규)
│       ├── PermissionServiceImpl.java         (신규)
│       └── MenuServiceImpl.java               (신규)

src/main/resources/mapper/                     ← 신규 XML 매퍼
├── UserMapper.xml
└── MenuMapper.xml
```

---

## Step 2-1. Entity 생성/수정

### 변경 요약

| Entity | 상태 | 매핑 테이블 | PK |
|--------|------|------------|-----|
| User | **수정** (기존 코드 대폭 변경) | `user` | `user_seq` |
| Role | 신규 | `role` | `role_seq` |
| Permission | 신규 | `permission` | `permission_seq` |
| Menu | 신규 | `menu` | `menu_seq` |
| UserRole | 신규 | `user_role` | 복합키 (user_seq, role_seq) |
| RolePermission | 신규 | `role_permission` | 복합키 (role_seq, permission_seq) |
| RoleMenu | 신규 | `role_menu` | 복합키 (role_seq, menu_seq) |

### 공통 규칙

```
모든 Entity 공통:
  - @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor
  - @TableName("테이블명")
  - PK: @TableId(value = "컬럼명", type = IdType.AUTO)
  - 날짜 필드: @TableField(fill = FieldFill.INSERT) 또는 @TableField(fill = FieldFill.INSERT_UPDATE)
  - underscore_case(DB) → camelCase(Java) 자동 변환 (application.properties에 설정 완료)
```

---

### 2-1-A. User.java (수정)

**파일 경로**: `src/main/java/com/aroon/business/entity/User.java`

> **중요**: 기존 User.java를 **전체 교체**합니다.
> 기존에 있던 `id`, `age` 필드가 제거되고, schema.sql의 user 테이블과 정확히 매핑되도록 변경합니다.

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
@TableName("user")
public class User {

    @TableId(value = "user_seq", type = IdType.AUTO)
    private Long userSeq;

    private String username;

    private String password;

    private String email;

    private String nickname;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

**기존 대비 변경사항:**

| 항목 | 기존 | 변경 후 |
|------|------|---------|
| PK 필드 | `id` | `userSeq` (DB: `user_seq`) |
| PK 어노테이션 | `@TableId(type = IdType.AUTO)` | `@TableId(value = "user_seq", type = IdType.AUTO)` |
| 필드 | username, email, age | username, password, email, nickname, status |
| 날짜 필드 | 없음 | createdAt, updatedAt (자동 채움) |

> **참고**: 기존 `UserServiceTest.java`는 이 변경으로 인해 컴파일 오류가 발생합니다.
> Phase 2 완료 후 테스트를 재작성해야 합니다. (Step 2-8 참조)

---

### 2-1-B. Role.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/entity/Role.java`

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
@TableName("role")
public class Role {

    @TableId(value = "role_seq", type = IdType.AUTO)
    private Long roleSeq;

    private String roleCode;

    private String roleName;

    private String description;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

---

### 2-1-C. Permission.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/entity/Permission.java`

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
@TableName("permission")
public class Permission {

    @TableId(value = "permission_seq", type = IdType.AUTO)
    private Long permissionSeq;

    private String permissionCode;

    private String permissionName;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

---

### 2-1-D. Menu.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/entity/Menu.java`

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
@TableName("menu")
public class Menu {

    @TableId(value = "menu_seq", type = IdType.AUTO)
    private Long menuSeq;

    private Long parentSeq;

    private String menuCode;

    private String menuName;

    private String menuType;

    private String path;

    private String icon;

    private Integer sortOrder;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

---

### 2-1-E. UserRole.java (신규 - 관계 테이블)

**파일 경로**: `src/main/java/com/aroon/business/entity/UserRole.java`

> 관계 테이블은 복합 PK를 사용하므로 `@TableId` 대신 일반 필드로 정의합니다.
> CRUD는 조건 기반(Wrapper)으로 처리합니다.

```java
package com.aroon.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_role")
public class UserRole {

    private Long userSeq;

    private Long roleSeq;
}
```

---

### 2-1-F. RolePermission.java (신규 - 관계 테이블)

**파일 경로**: `src/main/java/com/aroon/business/entity/RolePermission.java`

```java
package com.aroon.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("role_permission")
public class RolePermission {

    private Long roleSeq;

    private Long permissionSeq;
}
```

---

### 2-1-G. RoleMenu.java (신규 - 관계 테이블)

**파일 경로**: `src/main/java/com/aroon/business/entity/RoleMenu.java`

```java
package com.aroon.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("role_menu")
public class RoleMenu {

    private Long roleSeq;

    private Long menuSeq;
}
```

---

## Step 2-2. Mapper 인터페이스

### 공통 규칙

```
모든 Mapper 공통:
  - @Mapper 어노테이션
  - BaseMapper<Entity> 상속 → CRUD 자동 제공
  - 복잡한 조인 쿼리만 별도 메서드로 추가 (XML 매퍼에서 구현)
```

### 2-2-A. UserMapper.java (기존 유지)

**파일 경로**: `src/main/java/com/aroon/business/mapper/UserMapper.java`

> 기존 파일을 그대로 유지합니다. User 엔티티가 변경되었지만 Mapper는 BaseMapper 제네릭이므로 자동 반영됩니다.

```java
package com.aroon.business.mapper;

import com.aroon.business.entity.core.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

---

### 2-2-B. RoleMapper.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/mapper/RoleMapper.java`

```java
package com.aroon.business.mapper;

import com.aroon.business.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
```

---

### 2-2-C. PermissionMapper.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/mapper/PermissionMapper.java`

```java
package com.aroon.business.mapper;

import com.aroon.business.entity.Permission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}
```

---

### 2-2-D. MenuMapper.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/mapper/MenuMapper.java`

```java
package com.aroon.business.mapper;

import com.aroon.business.entity.Menu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
}
```

---

### 2-2-E. UserRoleMapper.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/mapper/UserRoleMapper.java`

```java
package com.aroon.business.mapper;

import com.aroon.business.entity.UserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
}
```

---

### 2-2-F. RolePermissionMapper.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/mapper/RolePermissionMapper.java`

```java
package com.aroon.business.mapper;

import com.aroon.business.entity.RolePermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}
```

---

### 2-2-G. RoleMenuMapper.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/mapper/RoleMenuMapper.java`

```java
package com.aroon.business.mapper;

import com.aroon.business.entity.RoleMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenu> {
}
```

---

## Step 2-3. DTO 생성

### DTO 설계 원칙

```
1. Request DTO: 클라이언트 입력 + @Valid 유효성 검증
   - Create용과 Update용을 분리 (필수값이 다를 수 있음)
   - 검증 메시지는 i18n 키 사용: message = "{validation.xxx.required}"

2. Response DTO: 클라이언트에게 반환할 데이터
   - 민감 정보(password 등) 제외
   - @Builder로 Service에서 쉽게 생성

3. Permission은 Request DTO 없이 Entity 직접 사용
   - 필드가 단순하고, 별도 검증이 불필요한 경우
   - 필요 시 나중에 DTO 추가 가능
```

### DTO ↔ Entity 변환 흐름

```
[요청]  Client → UserCreateRequest (DTO) → Service에서 User (Entity)로 변환 → Mapper로 DB 저장
[응답]  DB → Mapper가 User (Entity) 반환 → Service에서 UserResponse (DTO)로 변환 → Client
```

---

### 2-3-A. UserCreateRequest.java

**파일 경로**: `src/main/java/com/aroon/business/dto/request/UserCreateRequest.java`

```java
package com.aroon.business.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank(message = "{validation.username.required}")
    @Size(min = 3, max = 50, message = "{validation.username.size}")
    private String username;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, max = 100, message = "{validation.password.size}")
    private String password;

    @Email(message = "{validation.email.format}")
    private String email;

    private String nickname;
}
```

---

### 2-3-B. UserUpdateRequest.java

**파일 경로**: `src/main/java/com/aroon/business/dto/request/UserUpdateRequest.java`

> Update는 password를 선택적으로 변경하므로 @NotBlank 없이 nullable로 둡니다.

```java
package com.aroon.business.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @Size(min = 8, max = 100, message = "{validation.password.size}")
    private String password;

    @Email(message = "{validation.email.format}")
    private String email;

    private String nickname;

    private Integer status;
}
```

---

### 2-3-C. RoleCreateRequest.java

**파일 경로**: `src/main/java/com/aroon/business/dto/request/RoleCreateRequest.java`

```java
package com.aroon.business.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleCreateRequest {

    @NotBlank(message = "{validation.role.code.required}")
    @Size(max = 50)
    private String roleCode;

    @NotBlank(message = "{validation.role.name.required}")
    @Size(max = 100)
    private String roleName;

    @Size(max = 255)
    private String description;
}
```

---

### 2-3-D. RoleUpdateRequest.java

**파일 경로**: `src/main/java/com/aroon/business/dto/request/RoleUpdateRequest.java`

```java
package com.aroon.business.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleUpdateRequest {

    @Size(max = 100)
    private String roleName;

    @Size(max = 255)
    private String description;

    private Integer status;
}
```

---

### 2-3-E. MenuCreateRequest.java

**파일 경로**: `src/main/java/com/aroon/business/dto/request/MenuCreateRequest.java`

```java
package com.aroon.business.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MenuCreateRequest {

    private Long parentSeq;

    @NotBlank(message = "{validation.menu.code.required}")
    @Size(max = 50)
    private String menuCode;

    @NotBlank(message = "{validation.menu.name.required}")
    @Size(max = 100)
    private String menuName;

    @NotBlank(message = "{validation.menu.type.required}")
    private String menuType;

    @Size(max = 255)
    private String path;

    @Size(max = 50)
    private String icon;

    private Integer sortOrder;
}
```

---

### 2-3-F. MenuUpdateRequest.java

**파일 경로**: `src/main/java/com/aroon/business/dto/request/MenuUpdateRequest.java`

```java
package com.aroon.business.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MenuUpdateRequest {

    private Long parentSeq;

    @Size(max = 100)
    private String menuName;

    private String menuType;

    @Size(max = 255)
    private String path;

    @Size(max = 50)
    private String icon;

    private Integer sortOrder;

    private Integer status;
}
```

---

### 2-3-G. UserResponse.java

**파일 경로**: `src/main/java/com/aroon/business/dto/response/UserResponse.java`

> password는 절대 포함하지 않습니다.

```java
package com.aroon.business.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {

    private Long userSeq;
    private String username;
    private String email;
    private String nickname;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

### 2-3-H. RoleResponse.java

**파일 경로**: `src/main/java/com/aroon/business/dto/response/RoleResponse.java`

```java
package com.aroon.business.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RoleResponse {

    private Long roleSeq;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

### 2-3-I. PermissionResponse.java

**파일 경로**: `src/main/java/com/aroon/business/dto/response/PermissionResponse.java`

```java
package com.aroon.business.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PermissionResponse {

    private Long permissionSeq;
    private String permissionCode;
    private String permissionName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

### 2-3-J. MenuResponse.java

**파일 경로**: `src/main/java/com/aroon/business/dto/response/MenuResponse.java`

> `children` 필드를 포함하여 트리 구조 응답을 지원합니다.

```java
package com.aroon.business.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MenuResponse {

    private Long menuSeq;
    private Long parentSeq;
    private String menuCode;
    private String menuName;
    private String menuType;
    private String path;
    private String icon;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<MenuResponse> children;
}
```

---

## Step 2-4. User 모듈 CRUD

### 2-4-A. UserService.java (수정)

**파일 경로**: `src/main/java/com/aroon/business/service/UserService.java`

> **기존 파일을 전체 교체합니다.**
> Entity 직접 반환 → DTO 반환으로 변경합니다.

```java
package com.aroon.business.service;

import com.aroon.business.dto.request.UserCreateRequest;
import com.aroon.business.dto.request.UserUpdateRequest;
import com.aroon.business.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createUser(UserCreateRequest request);

    UserResponse getUserBySeq(Long userSeq);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(Long userSeq, UserUpdateRequest request);

    void deleteUser(Long userSeq);
}
```

---

### 2-4-B. UserServiceImpl.java (수정)

**파일 경로**: `src/main/java/com/aroon/business/service/impl/UserServiceImpl.java`

> **기존 파일을 전체 교체합니다.**
> Phase 1의 BusinessException, ErrorCode를 사용합니다.

```java
package com.aroon.business.service.impl;

import com.aroon.business.common.exception.BusinessException;
import com.aroon.business.common.exception.ErrorCode;
import com.aroon.business.dto.request.UserCreateRequest;
import com.aroon.business.dto.request.UserUpdateRequest;
import com.aroon.business.dto.response.UserResponse;
import com.aroon.business.entity.core.User;
import com.aroon.business.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final com.aroon.business.mapper.core.UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // username 중복 검사
        Long usernameCount = userMapper.selectCount(
                new LambdaQueryWrapper<com.aroon.business.entity.core.User>().eq(User::getUsername, request.getUsername())
        );
        if (usernameCount > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }

        // email 중복 검사 (email이 있는 경우만)
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            Long emailCount = userMapper.selectCount(
                    new LambdaQueryWrapper<com.aroon.business.entity.core.User>().eq(com.aroon.business.entity.core.User::getEmail, request.getEmail())
            );
            if (emailCount > 0) {
                throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
            }
        }

        // Entity 생성
        com.aroon.business.entity.core.User user = com.aroon.business.entity.core.User.builder()
                .username(request.getUsername())
                .password(request.getPassword())    // Phase 3에서 BCrypt 암호화 적용 예정
                .email(request.getEmail())
                .nickname(request.getNickname())
                .status(1)                           // 기본값: 활성
                .build();

        userMapper.insert(user);
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserBySeq(Long userSeq) {
        User user = userMapper.selectById(userSeq);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        List<com.aroon.business.entity.core.User> users = userMapper.selectList(null);
        return users.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userSeq, UserUpdateRequest request) {
        User user = userMapper.selectById(userSeq);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // email 중복 검사 (변경된 경우만)
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            Long emailCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(com.aroon.business.entity.core.User::getEmail, request.getEmail())
            );
            if (emailCount > 0) {
                throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
            }
            user.setEmail(request.getEmail());
        }

        // null이 아닌 필드만 업데이트 (선택적 수정)
        if (request.getPassword() != null) {
            user.setPassword(request.getPassword());  // Phase 3에서 BCrypt 적용
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        userMapper.updateById(user);
        return toResponse(userMapper.selectById(userSeq));
    }

    @Override
    @Transactional
    public void deleteUser(Long userSeq) {
        com.aroon.business.entity.core.User user = userMapper.selectById(userSeq);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userMapper.deleteById(userSeq);
    }

    // ── Entity → Response DTO 변환 ──
    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .userSeq(user.getUserSeq())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
```

---

### 2-4-C. UserController.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/controller/UserController.java`

```java
package com.aroon.business.controller;

import com.aroon.business.common.response.ApiResponse;
import com.aroon.business.dto.request.UserCreateRequest;
import com.aroon.business.dto.request.UserUpdateRequest;
import com.aroon.business.dto.response.UserResponse;
import com.aroon.business.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 사용자 목록 조회
     * GET /api/users
     */
    @GetMapping
    public ApiResponse<List<UserResponse>> getUsers() {
        return ApiResponse.success(userService.getAllUsers());
    }

    /**
     * 사용자 상세 조회
     * GET /api/users/{userSeq}
     */
    @GetMapping("/{userSeq}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long userSeq) {
        return ApiResponse.success(userService.getUserBySeq(userSeq));
    }

    /**
     * 사용자 생성
     * POST /api/users
     */
    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userService.createUser(request));
    }

    /**
     * 사용자 수정
     * PUT /api/users/{userSeq}
     */
    @PutMapping("/{userSeq}")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long userSeq,
                                                 @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.updateUser(userSeq, request));
    }

    /**
     * 사용자 삭제
     * DELETE /api/users/{userSeq}
     */
    @DeleteMapping("/{userSeq}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userSeq) {
        userService.deleteUser(userSeq);
        return ApiResponse.success();
    }
}
```

---

## Step 2-5. Role 모듈 CRUD

### 2-5-A. RoleService.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/service/RoleService.java`

```java
package com.aroon.business.service;

import com.aroon.business.dto.request.RoleCreateRequest;
import com.aroon.business.dto.request.RoleUpdateRequest;
import com.aroon.business.dto.response.RoleResponse;

import java.util.List;

public interface RoleService {

    RoleResponse createRole(RoleCreateRequest request);

    RoleResponse getRoleBySeq(Long roleSeq);

    List<RoleResponse> getAllRoles();

    RoleResponse updateRole(Long roleSeq, RoleUpdateRequest request);

    void deleteRole(Long roleSeq);
}
```

---

### 2-5-B. RoleServiceImpl.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/service/impl/RoleServiceImpl.java`

```java
package com.aroon.business.service.impl;

import com.aroon.business.common.exception.BusinessException;
import com.aroon.business.common.exception.ErrorCode;
import com.aroon.business.dto.request.RoleCreateRequest;
import com.aroon.business.dto.request.RoleUpdateRequest;
import com.aroon.business.dto.response.RoleResponse;
import com.aroon.business.entity.Role;
import com.aroon.business.mapper.RoleMapper;
import com.aroon.business.service.RoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;

    @Override
    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        // roleCode 중복 검사
        Long count = roleMapper.selectCount(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, request.getRoleCode())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_ROLE_CODE);
        }

        Role role = Role.builder()
                .roleCode(request.getRoleCode())
                .roleName(request.getRoleName())
                .description(request.getDescription())
                .status(1)
                .build();

        roleMapper.insert(role);
        return toResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleBySeq(Long roleSeq) {
        Role role = roleMapper.selectById(roleSeq);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return toResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        List<Role> roles = roleMapper.selectList(null);
        return roles.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public RoleResponse updateRole(Long roleSeq, RoleUpdateRequest request) {
        Role role = roleMapper.selectById(roleSeq);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }

        if (request.getRoleName() != null) {
            role.setRoleName(request.getRoleName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            role.setStatus(request.getStatus());
        }

        roleMapper.updateById(role);
        return toResponse(roleMapper.selectById(roleSeq));
    }

    @Override
    @Transactional
    public void deleteRole(Long roleSeq) {
        Role role = roleMapper.selectById(roleSeq);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        roleMapper.deleteById(roleSeq);
    }

    private RoleResponse toResponse(Role role) {
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
}
```

---

### 2-5-C. RoleController.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/controller/RoleController.java`

```java
package com.aroon.business.controller;

import com.aroon.business.common.response.ApiResponse;
import com.aroon.business.dto.request.RoleCreateRequest;
import com.aroon.business.dto.request.RoleUpdateRequest;
import com.aroon.business.dto.response.RoleResponse;
import com.aroon.business.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ApiResponse<List<RoleResponse>> getRoles() {
        return ApiResponse.success(roleService.getAllRoles());
    }

    @GetMapping("/{roleSeq}")
    public ApiResponse<RoleResponse> getRole(@PathVariable Long roleSeq) {
        return ApiResponse.success(roleService.getRoleBySeq(roleSeq));
    }

    @PostMapping
    public ApiResponse<RoleResponse> createRole(@Valid @RequestBody RoleCreateRequest request) {
        return ApiResponse.success(roleService.createRole(request));
    }

    @PutMapping("/{roleSeq}")
    public ApiResponse<RoleResponse> updateRole(@PathVariable Long roleSeq,
                                                 @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.success(roleService.updateRole(roleSeq, request));
    }

    @DeleteMapping("/{roleSeq}")
    public ApiResponse<Void> deleteRole(@PathVariable Long roleSeq) {
        roleService.deleteRole(roleSeq);
        return ApiResponse.success();
    }
}
```

---

## Step 2-6. Permission 모듈 CRUD

### 2-6-A. PermissionService.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/service/PermissionService.java`

> Permission은 필드가 단순하므로 Request DTO 없이 Entity를 직접 사용합니다.
> 필요 시 나중에 DTO를 추가해도 됩니다.

```java
package com.aroon.business.service;

import com.aroon.business.dto.response.PermissionResponse;
import com.aroon.business.entity.Permission;

import java.util.List;

public interface PermissionService {

    PermissionResponse createPermission(Permission permission);

    PermissionResponse getPermissionBySeq(Long permissionSeq);

    List<PermissionResponse> getAllPermissions();

    PermissionResponse updatePermission(Long permissionSeq, Permission permission);

    void deletePermission(Long permissionSeq);
}
```

---

### 2-6-B. PermissionServiceImpl.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/service/impl/PermissionServiceImpl.java`

```java
package com.aroon.business.service.impl;

import com.aroon.business.common.exception.BusinessException;
import com.aroon.business.common.exception.ErrorCode;
import com.aroon.business.dto.response.PermissionResponse;
import com.aroon.business.entity.Permission;
import com.aroon.business.mapper.PermissionMapper;
import com.aroon.business.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;

    @Override
    @Transactional
    public PermissionResponse createPermission(Permission permission) {
        permissionMapper.insert(permission);
        return toResponse(permission);
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionBySeq(Long permissionSeq) {
        Permission permission = permissionMapper.selectById(permissionSeq);
        if (permission == null) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        return toResponse(permission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        List<Permission> permissions = permissionMapper.selectList(null);
        return permissions.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public PermissionResponse updatePermission(Long permissionSeq, Permission permission) {
        Permission existing = permissionMapper.selectById(permissionSeq);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }

        if (permission.getPermissionName() != null) {
            existing.setPermissionName(permission.getPermissionName());
        }
        if (permission.getDescription() != null) {
            existing.setDescription(permission.getDescription());
        }

        permissionMapper.updateById(existing);
        return toResponse(permissionMapper.selectById(permissionSeq));
    }

    @Override
    @Transactional
    public void deletePermission(Long permissionSeq) {
        Permission permission = permissionMapper.selectById(permissionSeq);
        if (permission == null) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        permissionMapper.deleteById(permissionSeq);
    }

    private PermissionResponse toResponse(Permission permission) {
        return PermissionResponse.builder()
                .permissionSeq(permission.getPermissionSeq())
                .permissionCode(permission.getPermissionCode())
                .permissionName(permission.getPermissionName())
                .description(permission.getDescription())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .build();
    }
}
```

---

### 2-6-C. PermissionController.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/controller/PermissionController.java`

```java
package com.aroon.business.controller;

import com.aroon.business.common.response.ApiResponse;
import com.aroon.business.dto.response.PermissionResponse;
import com.aroon.business.entity.Permission;
import com.aroon.business.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ApiResponse<List<PermissionResponse>> getPermissions() {
        return ApiResponse.success(permissionService.getAllPermissions());
    }

    @GetMapping("/{permissionSeq}")
    public ApiResponse<PermissionResponse> getPermission(@PathVariable Long permissionSeq) {
        return ApiResponse.success(permissionService.getPermissionBySeq(permissionSeq));
    }

    @PostMapping
    public ApiResponse<PermissionResponse> createPermission(@RequestBody Permission permission) {
        return ApiResponse.success(permissionService.createPermission(permission));
    }

    @PutMapping("/{permissionSeq}")
    public ApiResponse<PermissionResponse> updatePermission(@PathVariable Long permissionSeq,
                                                             @RequestBody Permission permission) {
        return ApiResponse.success(permissionService.updatePermission(permissionSeq, permission));
    }

    @DeleteMapping("/{permissionSeq}")
    public ApiResponse<Void> deletePermission(@PathVariable Long permissionSeq) {
        permissionService.deletePermission(permissionSeq);
        return ApiResponse.success();
    }
}
```

---

## Step 2-7. Menu 모듈 CRUD (트리 구조 포함)

### 2-7-A. MenuService.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/service/MenuService.java`

> 메뉴는 트리 구조이므로 `getMenuTree()` 메서드가 추가됩니다.

```java
package com.aroon.business.service;

import com.aroon.business.dto.request.MenuCreateRequest;
import com.aroon.business.dto.request.MenuUpdateRequest;
import com.aroon.business.dto.response.MenuResponse;

import java.util.List;

public interface MenuService {

    MenuResponse createMenu(MenuCreateRequest request);

    MenuResponse getMenuBySeq(Long menuSeq);

    List<MenuResponse> getAllMenus();

    /**
     * 메뉴를 트리 구조로 반환
     * 최상위 메뉴(parentSeq = null) 아래에 하위 메뉴가 children으로 포함됨
     */
    List<MenuResponse> getMenuTree();

    MenuResponse updateMenu(Long menuSeq, MenuUpdateRequest request);

    void deleteMenu(Long menuSeq);
}
```

---

### 2-7-B. MenuServiceImpl.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/service/impl/MenuServiceImpl.java`

```java
package com.aroon.business.service.impl;

import com.aroon.business.common.exception.BusinessException;
import com.aroon.business.common.exception.ErrorCode;
import com.aroon.business.dto.request.MenuCreateRequest;
import com.aroon.business.dto.request.MenuUpdateRequest;
import com.aroon.business.dto.response.MenuResponse;
import com.aroon.business.entity.Menu;
import com.aroon.business.mapper.MenuMapper;
import com.aroon.business.service.MenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;

    @Override
    @Transactional
    public MenuResponse createMenu(MenuCreateRequest request) {
        Menu menu = Menu.builder()
                .parentSeq(request.getParentSeq())
                .menuCode(request.getMenuCode())
                .menuName(request.getMenuName())
                .menuType(request.getMenuType())
                .path(request.getPath())
                .icon(request.getIcon())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .status(1)
                .build();

        menuMapper.insert(menu);
        return toResponse(menu);
    }

    @Override
    @Transactional(readOnly = true)
    public MenuResponse getMenuBySeq(Long menuSeq) {
        Menu menu = menuMapper.selectById(menuSeq);
        if (menu == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
        return toResponse(menu);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuResponse> getAllMenus() {
        List<Menu> menus = menuMapper.selectList(
                new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSortOrder)
        );
        return menus.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuResponse> getMenuTree() {
        // 1. 전체 메뉴를 sortOrder 순으로 조회
        List<Menu> allMenus = menuMapper.selectList(
                new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSortOrder)
        );

        // 2. 모든 메뉴를 Response로 변환
        List<MenuResponse> allResponses = allMenus.stream()
                .map(this::toResponse)
                .toList();

        // 3. parentSeq 기준으로 그루핑
        Map<Long, List<MenuResponse>> childrenMap = allResponses.stream()
                .filter(menu -> menu.getParentSeq() != null)
                .collect(Collectors.groupingBy(MenuResponse::getParentSeq));

        // 4. 각 메뉴에 children 세팅
        allResponses.forEach(menu -> {
            List<MenuResponse> children = childrenMap.get(menu.getMenuSeq());
            if (children != null && !children.isEmpty()) {
                menu.setChildren(children);
            }
        });

        // 5. 최상위 메뉴(parentSeq == null)만 반환
        return allResponses.stream()
                .filter(menu -> menu.getParentSeq() == null)
                .toList();
    }

    @Override
    @Transactional
    public MenuResponse updateMenu(Long menuSeq, MenuUpdateRequest request) {
        Menu menu = menuMapper.selectById(menuSeq);
        if (menu == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }

        if (request.getParentSeq() != null) {
            menu.setParentSeq(request.getParentSeq());
        }
        if (request.getMenuName() != null) {
            menu.setMenuName(request.getMenuName());
        }
        if (request.getMenuType() != null) {
            menu.setMenuType(request.getMenuType());
        }
        if (request.getPath() != null) {
            menu.setPath(request.getPath());
        }
        if (request.getIcon() != null) {
            menu.setIcon(request.getIcon());
        }
        if (request.getSortOrder() != null) {
            menu.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            menu.setStatus(request.getStatus());
        }

        menuMapper.updateById(menu);
        return toResponse(menuMapper.selectById(menuSeq));
    }

    @Override
    @Transactional
    public void deleteMenu(Long menuSeq) {
        Menu menu = menuMapper.selectById(menuSeq);
        if (menu == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
        menuMapper.deleteById(menuSeq);
    }

    private MenuResponse toResponse(Menu menu) {
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
}
```

### 트리 구조 알고리즘 설명

```
DB에 저장된 상태 (flat):
┌──────────┬────────────┬──────────────┐
│ menu_seq │ parent_seq │ menu_name    │
├──────────┼────────────┼──────────────┤
│    1     │   NULL     │ 시스템 관리   │  ← 최상위
│    2     │     1      │ 사용자 관리   │  ← 1의 하위
│    3     │     1      │ 역할 관리     │  ← 1의 하위
│    4     │     1      │ 권한 관리     │  ← 1의 하위
│    5     │     1      │ 메뉴 관리     │  ← 1의 하위
└──────────┴────────────┴──────────────┘

getMenuTree() 응답 (tree):
[
  {
    "menuSeq": 1,
    "menuName": "시스템 관리",
    "parentSeq": null,
    "children": [
      { "menuSeq": 2, "menuName": "사용자 관리", "parentSeq": 1 },
      { "menuSeq": 3, "menuName": "역할 관리",   "parentSeq": 1 },
      { "menuSeq": 4, "menuName": "권한 관리",   "parentSeq": 1 },
      { "menuSeq": 5, "menuName": "메뉴 관리",   "parentSeq": 1 }
    ]
  }
]
```

---

### 2-7-C. MenuController.java (신규)

**파일 경로**: `src/main/java/com/aroon/business/controller/MenuController.java`

```java
package com.aroon.business.controller;

import com.aroon.business.common.response.ApiResponse;
import com.aroon.business.dto.request.MenuCreateRequest;
import com.aroon.business.dto.request.MenuUpdateRequest;
import com.aroon.business.dto.response.MenuResponse;
import com.aroon.business.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /**
     * 메뉴 트리 조회 (기본)
     * GET /api/menus
     */
    @GetMapping
    public ApiResponse<List<MenuResponse>> getMenuTree() {
        return ApiResponse.success(menuService.getMenuTree());
    }

    /**
     * 메뉴 전체 목록 조회 (flat)
     * GET /api/menus/list
     */
    @GetMapping("/list")
    public ApiResponse<List<MenuResponse>> getMenuList() {
        return ApiResponse.success(menuService.getAllMenus());
    }

    /**
     * 메뉴 상세 조회
     * GET /api/menus/{menuSeq}
     */
    @GetMapping("/{menuSeq}")
    public ApiResponse<MenuResponse> getMenu(@PathVariable Long menuSeq) {
        return ApiResponse.success(menuService.getMenuBySeq(menuSeq));
    }

    /**
     * 메뉴 생성
     * POST /api/menus
     */
    @PostMapping
    public ApiResponse<MenuResponse> createMenu(@Valid @RequestBody MenuCreateRequest request) {
        return ApiResponse.success(menuService.createMenu(request));
    }

    /**
     * 메뉴 수정
     * PUT /api/menus/{menuSeq}
     */
    @PutMapping("/{menuSeq}")
    public ApiResponse<MenuResponse> updateMenu(@PathVariable Long menuSeq,
                                                 @Valid @RequestBody MenuUpdateRequest request) {
        return ApiResponse.success(menuService.updateMenu(menuSeq, request));
    }

    /**
     * 메뉴 삭제
     * DELETE /api/menus/{menuSeq}
     */
    @DeleteMapping("/{menuSeq}")
    public ApiResponse<Void> deleteMenu(@PathVariable Long menuSeq) {
        menuService.deleteMenu(menuSeq);
        return ApiResponse.success();
    }
}
```

---

## Step 2-8. XML 매퍼 (조인 쿼리)

Phase 2의 기본 CRUD는 BaseMapper만으로 충분합니다.
아래 XML 매퍼는 Phase 4(관계 할당, 사용자별 메뉴 조회)에서 사용할 조인 쿼리를 **미리 준비**해 두는 것입니다.
지금은 빈 파일로 생성해두고, Phase 4에서 내용을 채웁니다.

### 2-8-A. UserMapper.xml

**파일 경로**: `src/main/resources/mapper/UserMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.aroon.business.mapper.core.UserMapper">

    <!-- Phase 4에서 사용자 + 역할 조인 조회 추가 예정 -->

</mapper>
```

### 2-8-B. MenuMapper.xml

**파일 경로**: `src/main/resources/mapper/MenuMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.aroon.business.mapper.MenuMapper">

    <!-- Phase 4에서 사용자별 메뉴 트리 조회 추가 예정 -->

</mapper>
```

---

## Step 2-9. 기존 테스트 대응

User 엔티티가 대폭 변경되었으므로 기존 `UserServiceTest.java`는 컴파일 오류가 발생합니다.

### 선택지

| 방법 | 설명 |
|------|------|
| A. 기존 테스트 삭제 후 재작성 | Phase 2 완료 후 새 구조에 맞게 재작성 |
| B. 기존 테스트 임시 비활성화 | `@Disabled` 어노테이션 추가하여 스킵 |

### 방법 A: 재작성 예시

**파일 경로**: `src/test/java/com/aroon/business/service/UserServiceTest.java`

```java
package com.aroon.business.service;

import com.aroon.business.common.exception.BusinessException;
import com.aroon.business.common.exception.ErrorCode;
import com.aroon.business.dto.request.UserCreateRequest;
import com.aroon.business.dto.request.UserUpdateRequest;
import com.aroon.business.dto.response.UserResponse;
import com.aroon.business.entity.core.User;
import com.aroon.business.mapper.core.UserMapper;
import com.aroon.business.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userSeq(1L)
                .username("testuser")
                .password("encoded_password")
                .email("test@example.com")
                .nickname("테스트유저")
                .status(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("사용자 생성 성공")
    void createUser_Success() {
        // given
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("new@example.com");
        request.setNickname("새유저");

        given(userMapper.selectCount(any())).willReturn(0L);
        given(userMapper.insert(any(User.class))).willReturn(1);

        // when
        UserResponse result = userService.createUser(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("newuser");
        verify(userMapper, times(1)).insert(any(User.class));
    }

    @Test
    @DisplayName("사용자 생성 실패 - username 중복")
    void createUser_DuplicateUsername() {
        // given
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("existinguser");
        request.setPassword("password123");

        given(userMapper.selectCount(any())).willReturn(1L);

        // when & then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_USERNAME);
                });
    }

    @Test
    @DisplayName("사용자 조회 성공")
    void getUserBySeq_Success() {
        // given
        given(userMapper.selectById(1L)).willReturn(testUser);

        // when
        UserResponse result = userService.getUserBySeq(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserSeq()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("사용자 조회 실패 - 존재하지 않음")
    void getUserBySeq_NotFound() {
        // given
        given(userMapper.selectById(999L)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> userService.getUserBySeq(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("전체 사용자 조회")
    void getAllUsers_Success() {
        // given
        User user2 = User.builder().userSeq(2L).username("user2").email("u2@test.com")
                .nickname("유저2").status(1).build();
        given(userMapper.selectList(null)).willReturn(Arrays.asList(testUser, user2));

        // when
        List<UserResponse> result = userService.getAllUsers();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserResponse::getUsername)
                .containsExactly("testuser", "user2");
    }

    @Test
    @DisplayName("사용자 수정 성공")
    void updateUser_Success() {
        // given
        UserUpdateRequest request = new UserUpdateRequest();
        request.setNickname("수정된닉네임");

        User updatedUser = User.builder()
                .userSeq(1L).username("testuser").email("test@example.com")
                .nickname("수정된닉네임").status(1).build();

        given(userMapper.selectById(1L)).willReturn(testUser, updatedUser);
        given(userMapper.updateById(any(User.class))).willReturn(1);

        // when
        UserResponse result = userService.updateUser(1L, request);

        // then
        assertThat(result.getNickname()).isEqualTo("수정된닉네임");
        verify(userMapper, times(1)).updateById(any(User.class));
    }

    @Test
    @DisplayName("사용자 삭제 성공")
    void deleteUser_Success() {
        // given
        given(userMapper.selectById(1L)).willReturn(testUser);
        given(userMapper.deleteById(1L)).willReturn(1);

        // when & then (예외 없이 완료)
        userService.deleteUser(1L);
        verify(userMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("사용자 삭제 실패 - 존재하지 않음")
    void deleteUser_NotFound() {
        // given
        given(userMapper.selectById(999L)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                });
    }
}
```

---

## API 테스트 가이드

Phase 2 완료 후 아래 curl 명령으로 각 API를 테스트할 수 있습니다.

### User API 테스트

```bash
# 사용자 목록 조회
curl http://localhost:8080/api/users

# 사용자 상세 조회
curl http://localhost:8080/api/users/1

# 사용자 생성
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test1234!","email":"test@aroon.com","nickname":"테스트"}'

# 사용자 수정
curl -X PUT http://localhost:8080/api/users/2 \
  -H "Content-Type: application/json" \
  -d '{"nickname":"수정닉네임","status":0}'

# 사용자 삭제
curl -X DELETE http://localhost:8080/api/users/2

# 유효성 검증 테스트 (username 누락 → 에러 반환)
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"password":"test1234!"}'

# 다국어 테스트 (존재하지 않는 사용자를 영어로 조회)
curl -H "Accept-Language: en" http://localhost:8080/api/users/999
```

### Role API 테스트

```bash
# 역할 목록 조회
curl http://localhost:8080/api/roles

# 역할 생성
curl -X POST http://localhost:8080/api/roles \
  -H "Content-Type: application/json" \
  -d '{"roleCode":"ROLE_VIEWER","roleName":"뷰어","description":"읽기만 가능"}'

# 역할 수정
curl -X PUT http://localhost:8080/api/roles/4 \
  -H "Content-Type: application/json" \
  -d '{"roleName":"뷰어(수정됨)"}'

# 역할 삭제
curl -X DELETE http://localhost:8080/api/roles/4
```

### Permission API 테스트

```bash
# 권한 목록 조회
curl http://localhost:8080/api/permissions

# 권한 생성
curl -X POST http://localhost:8080/api/permissions \
  -H "Content-Type: application/json" \
  -d '{"permissionCode":"report:read","permissionName":"리포트 조회","description":"리포트 조회 권한"}'
```

### Menu API 테스트

```bash
# 메뉴 트리 조회 (children 포함)
curl http://localhost:8080/api/menus

# 메뉴 전체 목록 조회 (flat)
curl http://localhost:8080/api/menus/list

# 메뉴 생성 (하위 메뉴)
curl -X POST http://localhost:8080/api/menus \
  -H "Content-Type: application/json" \
  -d '{"parentSeq":1,"menuCode":"system:log","menuName":"로그 관리","menuType":"MENU","path":"/system/log","icon":"log","sortOrder":5}'
```

---

## 체크리스트

### 파일 생성/수정 체크

| # | 파일 | 상태 | 확인 |
|---|------|------|------|
| 1 | `entity/User.java` | 수정 | ☐ |
| 2 | `entity/Role.java` | 신규 | ☐ |
| 3 | `entity/Permission.java` | 신규 | ☐ |
| 4 | `entity/Menu.java` | 신규 | ☐ |
| 5 | `entity/UserRole.java` | 신규 | ☐ |
| 6 | `entity/RolePermission.java` | 신규 | ☐ |
| 7 | `entity/RoleMenu.java` | 신규 | ☐ |
| 8 | `mapper/RoleMapper.java` | 신규 | ☐ |
| 9 | `mapper/PermissionMapper.java` | 신규 | ☐ |
| 10 | `mapper/MenuMapper.java` | 신규 | ☐ |
| 11 | `mapper/UserRoleMapper.java` | 신규 | ☐ |
| 12 | `mapper/RolePermissionMapper.java` | 신규 | ☐ |
| 13 | `mapper/RoleMenuMapper.java` | 신규 | ☐ |
| 14 | `dto/request/UserCreateRequest.java` | 신규 | ☐ |
| 15 | `dto/request/UserUpdateRequest.java` | 신규 | ☐ |
| 16 | `dto/request/RoleCreateRequest.java` | 신규 | ☐ |
| 17 | `dto/request/RoleUpdateRequest.java` | 신규 | ☐ |
| 18 | `dto/request/MenuCreateRequest.java` | 신규 | ☐ |
| 19 | `dto/request/MenuUpdateRequest.java` | 신규 | ☐ |
| 20 | `dto/response/UserResponse.java` | 신규 | ☐ |
| 21 | `dto/response/RoleResponse.java` | 신규 | ☐ |
| 22 | `dto/response/PermissionResponse.java` | 신규 | ☐ |
| 23 | `dto/response/MenuResponse.java` | 신규 | ☐ |
| 24 | `service/UserService.java` | 수정 | ☐ |
| 25 | `service/impl/UserServiceImpl.java` | 수정 | ☐ |
| 26 | `service/RoleService.java` | 신규 | ☐ |
| 27 | `service/impl/RoleServiceImpl.java` | 신규 | ☐ |
| 28 | `service/PermissionService.java` | 신규 | ☐ |
| 29 | `service/impl/PermissionServiceImpl.java` | 신규 | ☐ |
| 30 | `service/MenuService.java` | 신규 | ☐ |
| 31 | `service/impl/MenuServiceImpl.java` | 신규 | ☐ |
| 32 | `controller/UserController.java` | 신규 | ☐ |
| 33 | `controller/RoleController.java` | 신규 | ☐ |
| 34 | `controller/PermissionController.java` | 신규 | ☐ |
| 35 | `controller/MenuController.java` | 신규 | ☐ |
| 36 | `resources/mapper/UserMapper.xml` | 신규 | ☐ |
| 37 | `resources/mapper/MenuMapper.xml` | 신규 | ☐ |
| 38 | `test/.../UserServiceTest.java` | 수정 | ☐ |

### 빌드 및 기동 확인

```bash
# 컴파일 확인
./mvnw compile -DskipTests

# 테스트 실행
./mvnw test

# 서버 기동
./mvnw spring-boot:run
```

### API 동작 확인

```bash
# 최소 확인: 사용자 목록 조회 (admin 계정 1건 반환)
curl http://localhost:8080/api/users
# 기대: {"code":200,"message":"Success","data":[{"userSeq":1,"username":"admin",...}]}

# 최소 확인: 역할 목록 조회 (3건 반환)
curl http://localhost:8080/api/roles
# 기대: {"code":200,"message":"Success","data":[...3건...]}

# 최소 확인: 메뉴 트리 조회 (children 포함)
curl http://localhost:8080/api/menus
# 기대: {"code":200,"data":[{"menuSeq":1,"menuName":"시스템 관리","children":[...4건...]}]}
```

---

## 다음 단계: Phase 3 미리보기

Phase 2가 완료되면 Phase 3에서 인증/인가를 구현합니다.

| 작업 | 설명 |
|------|------|
| Security, JWT 의존성 추가 | pom.xml에 추가 |
| JwtTokenProvider | 토큰 생성/검증 유틸 |
| JwtAuthenticationFilter | 요청마다 토큰 검증 |
| SecurityConfig | 경로별 인증/인가 규칙 |
| UserDetailsServiceImpl | DB에서 사용자 인증 정보 로딩 |
| AuthController/Service | 로그인/로그아웃/토큰갱신 API |
| UserServiceImpl 수정 | password를 BCrypt로 암호화 |

Phase 2에서 `// Phase 3에서 BCrypt 적용`으로 표시한 부분이 Phase 3에서 변경됩니다.
