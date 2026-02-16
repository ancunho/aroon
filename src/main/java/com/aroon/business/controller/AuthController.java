package com.aroon.business.controller;

import com.aroon.business.common.exception.ErrorCode;
import com.aroon.business.common.response.ApiResponse;
import com.aroon.business.common.security.CustomUserDetails;
import com.aroon.business.dto.request.AdminLoginRequest;
import com.aroon.business.dto.request.LoginRequest;
import com.aroon.business.dto.request.RefreshTokenRequest;
import com.aroon.business.dto.response.TokenResponse;
import com.aroon.business.dto.response.UserResponse;
import com.aroon.business.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 로그인 (토큰 발급)
     */
    @PostMapping("/api/auth/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return ApiResponse.success(tokenResponse);
    }

    /**
     * 토큰 갱신
     */
    @PostMapping("/api/auth/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse tokenResponse = authService.refresh(request);
        return ApiResponse.success(tokenResponse);
    }

    /**
     * 내 정보 조회
     */
    @GetMapping("/api/auth/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponse userResponse = authService.getMyInfo(userDetails.getUserSeq());
        return ApiResponse.success(userResponse);
    }
}
