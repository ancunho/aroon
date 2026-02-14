# Junction 테이블 Entity-Schema 불일치 수정 계획

## 1. 문제 요약

DB 스키마의 Junction 테이블 3개(`user_role`, `role_permission`, `role_menu`)가 복합 PK 구조인데,
Java Entity에는 DB에 존재하지 않는 단일 PK 컬럼(`user_role_seq` 등)이 선언되어 있어
MyBatis-Plus의 기본 CRUD 메서드 호출 시 `Unknown column` SQL 에러가 발생한다.

## 2. 해결 방안

**방안 A**: DB 스키마에 AUTO_INCREMENT 단일 PK를 추가하고, 기존 복합 PK를 UNIQUE 제약으로 변경한다.

## 3. 수정 대상 파일

| # | 파일 | 수정 유형 |
|---|------|-----------|
| 1 | `docs/schema.sql` | DDL 변경 |
| 2 | (DB 직접 실행) | ALTER TABLE 실행 |

> Entity 파일(`UserRole.java`, `RolePermission.java`, `RoleMenu.java`)은 이미 단일 PK 구조로 되어 있으므로 **수정 불필요**.

---

## 4. 수정 상세 내용

### 4-1. `docs/schema.sql` — user_role 테이블

**변경 전**
```sql
CREATE TABLE `user_role` (
    `user_seq`   BIGINT NOT NULL COMMENT '사용자 SEQ',
    `role_seq`   BIGINT NOT NULL COMMENT '역할 SEQ',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (`user_seq`, `role_seq`),
    KEY `idx_user_role_user` (`user_seq`),
    KEY `idx_user_role_role` (`role_seq`)
);
```

**변경 후**
```sql
CREATE TABLE `user_role` (
    `user_role_seq` BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자-역할 SEQ',
    `user_seq`      BIGINT NOT NULL COMMENT '사용자 SEQ',
    `role_seq`      BIGINT NOT NULL COMMENT '역할 SEQ',
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (`user_role_seq`),
    UNIQUE KEY `uk_user_role` (`user_seq`, `role_seq`),
    KEY `idx_user_role_user` (`user_seq`),
    KEY `idx_user_role_role` (`role_seq`)
);
```

**변경 포인트**
- `user_role_seq` BIGINT AUTO_INCREMENT 컬럼 추가
- PRIMARY KEY를 `(user_seq, role_seq)` → `(user_role_seq)`로 변경
- 기존 복합 PK를 `UNIQUE KEY uk_user_role (user_seq, role_seq)`로 변경 (중복 방지 유지)

---

### 4-2. `docs/schema.sql` — role_permission 테이블

**변경 전**
```sql
CREATE TABLE `role_permission` (
    `role_seq`       BIGINT NOT NULL COMMENT '역할 SEQ',
    `permission_seq` BIGINT NOT NULL COMMENT '권한 SEQ',
    `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    `updated_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (`role_seq`, `permission_seq`),
    KEY `idx_role_permission_role` (`role_seq`),
    KEY `idx_role_permission_permission` (`permission_seq`)
);
```

**변경 후**
```sql
CREATE TABLE `role_permission` (
    `role_permission_seq` BIGINT NOT NULL AUTO_INCREMENT COMMENT '역할-권한 SEQ',
    `role_seq`            BIGINT NOT NULL COMMENT '역할 SEQ',
    `permission_seq`      BIGINT NOT NULL COMMENT '권한 SEQ',
    `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    `updated_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (`role_permission_seq`),
    UNIQUE KEY `uk_role_permission` (`role_seq`, `permission_seq`),
    KEY `idx_role_permission_role` (`role_seq`),
    KEY `idx_role_permission_permission` (`permission_seq`)
);
```

**변경 포인트**
- `role_permission_seq` BIGINT AUTO_INCREMENT 컬럼 추가
- PRIMARY KEY를 `(role_seq, permission_seq)` → `(role_permission_seq)`로 변경
- 기존 복합 PK를 `UNIQUE KEY uk_role_permission (role_seq, permission_seq)`로 변경

---

### 4-3. `docs/schema.sql` — role_menu 테이블

**변경 전**
```sql
CREATE TABLE `role_menu` (
    `role_seq` BIGINT NOT NULL COMMENT '역할 SEQ',
    `menu_seq` BIGINT NOT NULL COMMENT '메뉴 SEQ',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (`role_seq`, `menu_seq`),
    KEY `idx_role_menu_role` (`role_seq`),
    KEY `idx_role_menu_menu` (`menu_seq`)
);
```

**변경 후**
```sql
CREATE TABLE `role_menu` (
    `role_menu_seq` BIGINT NOT NULL AUTO_INCREMENT COMMENT '역할-메뉴 SEQ',
    `role_seq`      BIGINT NOT NULL COMMENT '역할 SEQ',
    `menu_seq`      BIGINT NOT NULL COMMENT '메뉴 SEQ',
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (`role_menu_seq`),
    UNIQUE KEY `uk_role_menu` (`role_seq`, `menu_seq`),
    KEY `idx_role_menu_role` (`role_seq`),
    KEY `idx_role_menu_menu` (`menu_seq`)
);
```

**변경 포인트**
- `role_menu_seq` BIGINT AUTO_INCREMENT 컬럼 추가
- PRIMARY KEY를 `(role_seq, menu_seq)` → `(role_menu_seq)`로 변경
- 기존 복합 PK를 `UNIQUE KEY uk_role_menu (role_seq, menu_seq)`로 변경

---

## 5. 기존 DB에 이미 데이터가 있는 경우 (마이그레이션 SQL)

schema.sql을 처음부터 새로 실행하는 경우는 위 DDL만 적용하면 된다.
이미 운영 중인 DB에 데이터가 있다면 아래 ALTER TABLE로 마이그레이션한다.

```sql
-- user_role
ALTER TABLE `user_role` DROP PRIMARY KEY;
ALTER TABLE `user_role` ADD COLUMN `user_role_seq` BIGINT NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`user_role_seq`);
ALTER TABLE `user_role` ADD UNIQUE KEY `uk_user_role` (`user_seq`, `role_seq`);

-- role_permission
ALTER TABLE `role_permission` DROP PRIMARY KEY;
ALTER TABLE `role_permission` ADD COLUMN `role_permission_seq` BIGINT NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`role_permission_seq`);
ALTER TABLE `role_permission` ADD UNIQUE KEY `uk_role_permission` (`role_seq`, `permission_seq`);

-- role_menu
ALTER TABLE `role_menu` DROP PRIMARY KEY;
ALTER TABLE `role_menu` ADD COLUMN `role_menu_seq` BIGINT NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`role_menu_seq`);
ALTER TABLE `role_menu` ADD UNIQUE KEY `uk_role_menu` (`role_seq`, `menu_seq`);
```

## 6. Entity 파일 확인 (수정 불필요)

현재 Entity는 이미 단일 PK 구조로 선언되어 있어 DB 스키마만 맞추면 정상 동작한다.

| Entity | 현재 코드 | DB 수정 후 매핑 |
|--------|-----------|-----------------|
| `UserRole.java:16` | `@TableId(value = "user_role_seq", type = IdType.AUTO)` | user_role_seq 컬럼과 매핑 |
| `RolePermission.java:16` | `@TableId(value = "role_permission_seq", type = IdType.AUTO)` | role_permission_seq 컬럼과 매핑 |
| `RoleMenu.java:16` | `@TableId(value = "role_menu_seq", type = IdType.AUTO)` | role_menu_seq 컬럼과 매핑 |

## 7. 수정 후 검증 항목

- [ ] schema.sql의 3개 테이블 DDL이 변경되었는지 확인
- [ ] DB에 ALTER TABLE 또는 schema.sql 재실행 완료
- [ ] 애플리케이션 기동 후 `userRoleMapper.insert()` 정상 동작 확인
- [ ] `userRoleMapper.selectById()` 정상 동작 확인
- [ ] `userRoleMapper.deleteById()` 정상 동작 확인
- [ ] UNIQUE 제약으로 동일한 (user_seq, role_seq) 중복 INSERT 시 에러 발생 확인
