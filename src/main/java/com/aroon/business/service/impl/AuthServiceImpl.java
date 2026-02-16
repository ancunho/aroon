package com.aroon.business.service.impl;

import com.aroon.business.dto.request.AdminLoginRequest;
import com.aroon.business.service.AuthService;
import com.aroon.business.common.exception.BusinessException;
import com.aroon.business.common.exception.ErrorCode;
import com.aroon.business.common.security.JwtTokenProvider;
import com.aroon.business.dto.request.LoginRequest;
import com.aroon.business.dto.request.RefreshTokenRequest;
import com.aroon.business.dto.response.TokenResponse;
import com.aroon.business.dto.response.UserResponse;
import com.aroon.business.entity.core.Role;
import com.aroon.business.entity.core.User;
import com.aroon.business.entity.core.UserRole;
import com.aroon.business.mapper.core.RoleMapper;
import com.aroon.business.mapper.core.UserMapper;
import com.aroon.business.mapper.core.UserRoleMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        // 1. 사용자 조회
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUserId, request.getUserId())
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 2. 계정 상태 확인
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "비활성화된 계정입니다");
        }
        if ("Y".equals(user.getLoginLockYn())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "계정이 잠겨있습니다");
        }

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 4. 역할 조회
        List<String> roles = getUserRoles(user.getUserSeq());

        // 5. 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getUserSeq(), user.getUserId(), roles
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // 6. 마지막 로그인 시간 업데이트
        user.setLastLoginDate(LocalDateTime.now());
        userMapper.updateById(user);

        // 7. 응답
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(1800)
                .build();
    }

    @Override
    public TokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. Refresh Token 검증
        try {
            jwtTokenProvider.validateToken(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 2. 토큰 타입 확인
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 3. userId 추출 후 사용자 조회
        String userId = jwtTokenProvider.getUserId(refreshToken);
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUserId, userId)
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 4. 역할 조회
        List<String> roles = getUserRoles(user.getUserSeq());

        // 5. 새 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(
                user.getUserSeq(), user.getUserId(), roles
        );
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(1800)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyInfo(Long userSeq) {
        User user = userMapper.selectById(userSeq);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return UserResponse.builder()
                .userSeq(user.getUserSeq())
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * 사용자의 역할 코드 목록 조회
     */
    private List<String> getUserRoles(Long userSeq) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserSeq, userSeq)
        );
        List<Long> roleSeqs = userRoles.stream()
                .map(UserRole::getRoleSeq)
                .toList();

        if (roleSeqs.isEmpty()) {
            return List.of();
        }

        return roleMapper.selectBatchIds(roleSeqs).stream()
                .map(Role::getRoleCode)
                .toList();
    }
}
