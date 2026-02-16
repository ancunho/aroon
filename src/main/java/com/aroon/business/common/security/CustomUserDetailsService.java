package com.aroon.business.common.security;

import com.aroon.business.entity.core.Role;
import com.aroon.business.entity.core.User;
import com.aroon.business.entity.core.UserRole;
import com.aroon.business.mapper.core.RoleMapper;
import com.aroon.business.mapper.core.UserMapper;
import com.aroon.business.mapper.core.UserRoleMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        // 1. userId로 사용자 조회
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUserId, userId)
        );
        if (user == null) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId);
        }

        // 2. 사용자의 역할 조회
        List<UserRole> userRoles = userRoleMapper.selectList(
                Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserSeq, user.getUserSeq())
        );
        List<Long> roleSeqs = userRoles.stream()
                .map(UserRole::getRoleSeq)
                .toList();

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (!roleSeqs.isEmpty()) {
            List<Role> roles = roleMapper.selectBatchIds(roleSeqs);
            authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority(role.getRoleCode()))
                    .toList();
        }

        // 3. Spring Security UserDetails 반환
        return new CustomUserDetails(user, authorities);
    }
}
