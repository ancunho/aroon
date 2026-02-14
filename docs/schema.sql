-- =====================================================
-- Aroon RBAC Database Schema
-- Database: MySQL 8.x
-- =====================================================

-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS aroon DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE aroon;

-- =====================================================
-- 1. 사용자 테이블
-- =====================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `user_seq` BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 SEQ',
    `user_id` VARCHAR(50) NOT NULL COMMENT '로그인 ID',
    `password` VARCHAR(255) NOT NULL COMMENT '비밀번호 (BCrypt)',
    `email` VARCHAR(100) NULL COMMENT '이메일',
    `nickname` VARCHAR(50) NULL COMMENT '닉네임',
    `login_lock_yn` VARCHAR(1) NULL DEFAULT 'N' COMMENT '잠김여부',
    `mobile` VARCHAR(100) NULL COMMENT '전화번호',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '상태 (0:비활성, 1:활성)',
    `last_login_date` DATETIME NULL COMMENT '마지막 로그인 시간',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (`user_seq`),
    UNIQUE KEY `uk_user_username` (`user_id`),
    UNIQUE KEY `uk_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자';

-- =====================================================
-- 2. 역할 테이블
-- =====================================================
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
    `role_seq` BIGINT NOT NULL AUTO_INCREMENT COMMENT '역할 SEQ',
    `role_code` VARCHAR(50) NOT NULL COMMENT '역할 코드',
    `role_name` VARCHAR(100) NOT NULL COMMENT '역할 이름',
    `description` VARCHAR(255) NULL COMMENT '설명',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '상태 (0:비활성, 1:활성)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (`role_seq`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='역할';

-- =====================================================
-- 3. 권한 테이블
-- =====================================================
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
    `permission_seq` BIGINT NOT NULL AUTO_INCREMENT COMMENT '권한 SEQ',
    `permission_code` VARCHAR(100) NOT NULL COMMENT '권한 코드',
    `permission_name` VARCHAR(100) NOT NULL COMMENT '권한 이름',
    `description` VARCHAR(255) NULL COMMENT '설명',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (`permission_seq`),
    UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='권한';

-- =====================================================
-- 4. 메뉴 테이블
-- =====================================================
DROP TABLE IF EXISTS `menu`;
CREATE TABLE `menu` (
    `menu_seq` BIGINT NOT NULL AUTO_INCREMENT COMMENT '메뉴 SEQ',
    `parent_seq` BIGINT NULL COMMENT '상위 메뉴 SEQ',
    `menu_code` VARCHAR(50) NOT NULL COMMENT '메뉴 코드',
    `menu_name` VARCHAR(100) NOT NULL COMMENT '메뉴 이름',
    `menu_type` VARCHAR(20) NOT NULL COMMENT '메뉴 유형 (DIRECTORY/MENU/BUTTON)',
    `path` VARCHAR(255) NULL COMMENT '라우트 경로',
    `icon` VARCHAR(50) NULL COMMENT '아이콘',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '정렬 순서',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '상태 (0:비활성, 1:활성)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (`menu_seq`),
    UNIQUE KEY `uk_menu_code` (`menu_code`),
    KEY `idx_menu_parent` (`parent_seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='메뉴';

-- =====================================================
-- 5. 사용자-역할 관계 테이블
-- =====================================================
DROP TABLE IF EXISTS `user_role`;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자-역할 관계';
-- =====================================================
-- 6. 역할-권한 관계 테이블
-- =====================================================
DROP TABLE IF EXISTS `role_permission`;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='역할-권한 관계';
-- =====================================================
-- 7. 역할-메뉴 관계 테이블
-- =====================================================
DROP TABLE IF EXISTS `role_menu`;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='역할-메뉴 관계';
-- =====================================================
-- 초기 데이터 삽입
-- =====================================================

-- 기본 역할
INSERT INTO `role` (`role_code`, `role_name`, `description`) VALUES
('ROLE_ADMIN', '관리자', '시스템 전체 관리 권한'),
('ROLE_MANAGER', '매니저', '일반 관리 권한'),
('ROLE_USER', '일반 사용자', '기본 사용자 권한');

-- 기본 권한
INSERT INTO `permission` (`permission_code`, `permission_name`, `description`) VALUES
-- 사용자 권한
('user:read', '사용자 조회', '사용자 목록 및 상세 조회'),
('user:create', '사용자 생성', '새 사용자 등록'),
('user:update', '사용자 수정', '사용자 정보 수정'),
('user:delete', '사용자 삭제', '사용자 삭제'),
-- 역할 권한
('role:read', '역할 조회', '역할 목록 및 상세 조회'),
('role:create', '역할 생성', '새 역할 등록'),
('role:update', '역할 수정', '역할 정보 수정'),
('role:delete', '역할 삭제', '역할 삭제'),
-- 권한 관리
('permission:read', '권한 조회', '권한 목록 및 상세 조회'),
('permission:create', '권한 생성', '새 권한 등록'),
('permission:update', '권한 수정', '권한 정보 수정'),
('permission:delete', '권한 삭제', '권한 삭제'),
-- 메뉴 권한
('menu:read', '메뉴 조회', '메뉴 목록 및 상세 조회'),
('menu:create', '메뉴 생성', '새 메뉴 등록'),
('menu:update', '메뉴 수정', '메뉴 정보 수정'),
('menu:delete', '메뉴 삭제', '메뉴 삭제');

-- ADMIN 역할에 모든 권한 부여
INSERT INTO `role_permission` (`role_seq`, `permission_seq`)
SELECT 1, permission_seq FROM `permission`;

-- MANAGER 역할에 조회/수정 권한 부여 (삭제 권한 제외)
INSERT INTO `role_permission` (`role_seq`, `permission_seq`)
SELECT 2, permission_seq FROM `permission` WHERE `permission_code` NOT LIKE '%:delete';

-- USER 역할에 조회 권한만 부여
INSERT INTO `role_permission` (`role_seq`, `permission_seq`)
SELECT 3, permission_seq FROM `permission` WHERE `permission_code` LIKE '%:read';

-- 기본 메뉴
INSERT INTO `menu` (`parent_seq`, `menu_code`, `menu_name`, `menu_type`, `path`, `icon`, `sort_order`) VALUES
-- 시스템 관리 (디렉토리)
(NULL, 'system', '시스템 관리', 'DIRECTORY', '/system', 'setting', 1),
-- 시스템 관리 하위 메뉴
(1, 'system:user', '사용자 관리', 'MENU', '/system/user', 'user', 1),
(1, 'system:role', '역할 관리', 'MENU', '/system/role', 'peoples', 2),
(1, 'system:permission', '권한 관리', 'MENU', '/system/permission', 'lock', 3),
(1, 'system:menu', '메뉴 관리', 'MENU', '/system/menu', 'tree-table', 4);

-- ADMIN 역할에 모든 메뉴 권한 부여
INSERT INTO `role_menu` (`role_seq`, `menu_seq`)
SELECT 1, menu_seq FROM `menu`;

-- 관리자 계정 (password: admin123 - BCrypt 암호화)
INSERT INTO `user` (`user_id`, `password`, `email`, `nickname`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@aroon.com', '관리자');

-- 관리자에게 ADMIN 역할 부여
INSERT INTO `user_role` (`user_seq`, `role_seq`) VALUES (1, 1);


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