package com.aroon.business.service.impl;

import com.aroon.business.common.exception.BusinessException;
import com.aroon.business.common.exception.ErrorCode;
import com.aroon.business.dto.request.UserCreateRequest;
import com.aroon.business.dto.request.UserUpdateRequest;
import com.aroon.business.dto.response.UserResponse;
import com.aroon.business.entity.core.User;
import com.aroon.business.mapper.core.UserMapper;
import com.aroon.business.service.UserService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // user id 중복 검사
        Long usernameCount = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUserId, request.getUserId())
        );
        if (usernameCount > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }

        // email 중복 검사 (email이 있는 경우만)
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            Long emailCount = userMapper.selectCount(
                    Wrappers.<User>lambdaQuery().eq(User::getEmail, request.getEmail())
            );
            if (emailCount > 0) {
                throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
            }
        }

        // Entity 생성
        User user = User.builder()
                .userId(request.getUserId())
                .password(passwordEncoder.encode(request.getPassword()))
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
        List<User> users = userMapper.selectList(null);
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
                    Wrappers.<User>lambdaQuery().eq(User::getEmail, request.getEmail())
            );
            if (emailCount > 0) {
                throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
            }
            user.setEmail(request.getEmail());
        }

        // null이 아닌 필드만 업데이트 (선택적 수정)
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
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
        User user = userMapper.selectById(userSeq);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userMapper.deleteById(userSeq);
    }

    // ── Entity → Response DTO 변환 ──
    private UserResponse toResponse(User user) {
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
}
