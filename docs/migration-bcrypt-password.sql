-- =====================================================
-- 비밀번호 BCrypt 마이그레이션 (일회성 실행)
-- =====================================================
-- JWT 인증 도입으로 비밀번호가 BCrypt 해시로 저장되어야 합니다.
-- 이 스크립트는 기존 DB의 admin 비밀번호를 유효한 BCrypt 해시로 업데이트합니다.
--
-- 실행: mysql -u root -p aroon < docs/migration-bcrypt-password.sql
-- =====================================================

USE aroon;

-- admin 계정 비밀번호 업데이트 (admin123d → BCrypt 해시)
UPDATE `user`
SET `password` = '$2a$10$71Q5CaWyFpGkJJAXcEf3x.rHLHvI5wqstcLKIr5gPo3OgGze4I8IO'
WHERE `user_id` = 'admin';
