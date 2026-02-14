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
