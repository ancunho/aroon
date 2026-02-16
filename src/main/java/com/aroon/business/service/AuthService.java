package com.aroon.business.service;

import com.aroon.business.dto.request.AdminLoginRequest;
import com.aroon.business.dto.request.LoginRequest;
import com.aroon.business.dto.request.RefreshTokenRequest;
import com.aroon.business.dto.response.TokenResponse;
import com.aroon.business.dto.response.UserResponse;

public interface AuthService {

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    UserResponse getMyInfo(Long userSeq);
}
