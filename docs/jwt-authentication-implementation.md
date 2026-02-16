# JWT 토큰 기반 인증 구현 계획서

## 목차
1. [개요](#1-개요)
2. [인증 흐름 (Flow)](#2-인증-흐름-flow)
3. [추가 의존성](#3-추가-의존성)
4. [설정 (Configuration)](#4-설정-configuration)
5. [구현 상세](#5-구현-상세)
6. [기존 코드 수정사항](#6-기존-코드-수정사항)
7. [API 명세](#7-api-명세)
8. [파일 구조](#8-파일-구조)
9. [구현 순서](#9-구현-순서)
10. [테스트 가이드](#10-테스트-가이드)

---

## 1. 개요

### 현재 상태
- Spring Security 미적용 (의존성 없음)
- 비밀번호 평문 저장 (`UserServiceImpl`에서 BCrypt 미적용)
- 모든 API 엔드포인트가 인증 없이 접근 가능
- RBAC 테이블 구조는 설계 완료 (User, Role, Permission, UserRole, RolePermission)
- ErrorCode에 인증 관련 코드 준비됨 (INVALID_CREDENTIALS, TOKEN_EXPIRED, INVALID_TOKEN)

### 목표
- **Access Token + Refresh Token** 방식의 JWT 인증 구현
- 로그인 시 토큰 발급 → 이후 API 호출 시 토큰으로 인증
- 기존 RBAC 모델과 연동하여 역할/권한 기반 접근 제어

### 토큰 전략
| 항목 | Access Token | Refresh Token |
|------|-------------|---------------|
| 용도 | API 인증 | Access Token 재발급 |
| 유효시간 | 30분 | 7일 |
| 저장위치 (클라이언트) | 메모리 / LocalStorage | HttpOnly Cookie 또는 LocalStorage |
| 전달방식 | `Authorization: Bearer {token}` | POST body로 전달 |

---

## 2. 인증 흐름 (Flow)

### 2.1 로그인 (토큰 발급)
```
Client                          Server
  │                               │
  │  POST /api/auth/login         │
  │  { userId, password }         │
  │ ─────────────────────────────>│
  │                               │  1. userId로 User 조회
  │                               │  2. BCrypt로 password 검증
  │                               │  3. User의 Role/Permission 조회
  │                               │  4. Access Token 생성 (30분)
  │                               │  5. Refresh Token 생성 (7일)
  │                               │  6. lastLoginDate 업데이트
  │  {                            │
  │    accessToken,               │
  │    refreshToken,              │
  │    tokenType: "Bearer",       │
  │    expiresIn: 1800            │
  │  }                            │
  │ <─────────────────────────────│
```

### 2.2 API 호출 (토큰 인증)
```
Client                          Server
  │                               │
  │  GET /api/users               │
  │  Authorization: Bearer {AT}   │
  │ ─────────────────────────────>│
  │                               │  1. JwtAuthenticationFilter 동작
  │                               │  2. Authorization 헤더에서 토큰 추출
  │                               │  3. 토큰 유효성 검증 (서명, 만료)
  │                               │  4. 토큰에서 userId, roles 추출
  │                               │  5. SecurityContext에 인증 정보 설정
  │                               │  6. Controller 진입 → 정상 응답
  │  { code: 200, data: [...] }   │
  │ <─────────────────────────────│
```

### 2.3 토큰 만료 시 갱신
```
Client                          Server
  │                               │
  │  GET /api/users               │
  │  Authorization: Bearer {AT}   │
  │ ─────────────────────────────>│
  │                               │  토큰 만료 확인
  │  { code: 5002,                │
  │    message: "토큰 만료" }      │
  │ <─────────────────────────────│
  │                               │
  │  POST /api/auth/refresh       │
  │  { refreshToken: "{RT}" }     │
  │ ─────────────────────────────>│
  │                               │  1. Refresh Token 검증
  │                               │  2. 새로운 Access Token 발급
  │                               │  3. (선택) 새로운 Refresh Token 발급
  │  {                            │
  │    accessToken: "새 AT",       │
  │    refreshToken: "새 RT",      │
  │    tokenType: "Bearer",       │
  │    expiresIn: 1800            │
  │  }                            │
  │ <─────────────────────────────│
```

### 2.4 인증 실패 케이스
```
토큰 없음        → 401 (UNAUTHORIZED)         "인증이 필요합니다"
토큰 만료        → 401 (TOKEN_EXPIRED: 5002)   "토큰이 만료되었습니다"
토큰 위변조      → 401 (INVALID_TOKEN: 5003)   "유효하지 않은 토큰입니다"
권한 부족        → 403 (FORBIDDEN)             "접근 권한이 없습니다"
로그인 실패      → 401 (INVALID_CREDENTIALS: 5001) "아이디 또는 비밀번호가 올바르지 않습니다"
계정 잠금        → 401                         "계정이 잠겨있습니다"
```

---

## 3. 추가 의존성

`pom.xml`에 다음 의존성을 추가한다.

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT (jjwt) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- Spring Security Test -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 4. 설정 (Configuration)

### 4.1 application.properties에 JWT 설정 추가

```properties
# ── JWT 설정 ──
# Secret Key (운영 환경에서는 환경변수나 Vault에서 관리할 것)
# 최소 256비트(32바이트) 이상의 문자열 사용 (HS256 알고리즘)
jwt.secret=aroon-jwt-secret-key-must-be-at-least-256-bits-long-for-security
jwt.access-token-expiration=1800000
jwt.refresh-token-expiration=604800000
```

| 속성 | 설명 | 값 |
|------|------|----|
| `jwt.secret` | HMAC-SHA256 서명 키 | 32바이트 이상 문자열 |
| `jwt.access-token-expiration` | Access Token 만료 시간 (ms) | 1800000 (30분) |
| `jwt.refresh-token-expiration` | Refresh Token 만료 시간 (ms) | 604800000 (7일) |

### 4.2 JwtProperties 클래스

```java
package com.aroon.business.common.config;

@Getter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;

    // setter는 Spring이 properties 바인딩 시 사용
    public void setSecret(String secret) { this.secret = secret; }
    public void setAccessTokenExpiration(long accessTokenExpiration) { this.accessTokenExpiration = accessTokenExpiration; }
    public void setRefreshTokenExpiration(long refreshTokenExpiration) { this.refreshTokenExpiration = refreshTokenExpiration; }
}
```

---

## 5. 구현 상세

### 5.1 JwtTokenProvider — 토큰 생성/검증 유틸

JWT 토큰의 생성, 검증, 파싱을 담당하는 핵심 컴포넌트.

```java
package com.aroon.business.common.security;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey key;

    @PostConstruct
    public void init() {
        // secret 문자열로 HMAC-SHA 키 생성
        this.key = Keys.hmacShaKeyFor(
            jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Access Token 생성
     * Claims에 userId, userSeq, roles 포함
     */
    public String createAccessToken(Long userSeq, String userId, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(userId)
                .claim("userSeq", userSeq)
                .claim("roles", roles)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Refresh Token 생성
     * 최소한의 정보만 포함 (userId만)
     */
    public String createRefreshToken(String userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(userId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 토큰에서 Claims 추출
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 토큰에서 userId 추출
     */
    public String getUserId(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 토큰에서 roles 추출
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return getClaims(token).get("roles", List.class);
    }

    /**
     * 토큰 유효성 검증
     * @return true: 유효, false: 무효
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT 토큰 만료: {}", e.getMessage());
            throw e;  // 만료는 별도 처리 필요 (5002 에러코드)
        } catch (JwtException e) {
            log.warn("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰 타입 확인 (access / refresh)
     */
    public String getTokenType(String token) {
        return getClaims(token).get("type", String.class);
    }
}
```

### 5.2 CustomUserDetailsService — 사용자 인증 정보 로드

Spring Security의 `UserDetailsService` 구현. DB에서 사용자와 역할 정보를 조회한다.

```java
package com.aroon.business.common.security;

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
```

### 5.3 CustomUserDetails — UserDetails 구현체

```java
package com.aroon.business.common.security;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userSeq;
    private final String userId;
    private final String password;
    private final boolean accountLocked;
    private final Integer status;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user, Collection<? extends GrantedAuthority> authorities) {
        this.userSeq = user.getUserSeq();
        this.userId = user.getUserId();
        this.password = user.getPassword();
        this.accountLocked = "Y".equals(user.getLoginLockYn());
        this.status = user.getStatus();
        this.authorities = authorities;
    }

    @Override
    public String getUsername() { return userId; }

    @Override
    public String getPassword() { return password; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public boolean isAccountNonLocked() { return !accountLocked; }

    @Override
    public boolean isEnabled() { return status != null && status == 1; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }
}
```

### 5.4 JwtAuthenticationFilter — 요청마다 토큰 검증

모든 HTTP 요청을 가로채서 JWT 토큰을 검증하는 필터.

```java
package com.aroon.business.common.security;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    String userId = jwtTokenProvider.getUserId(token);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                        );
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (ExpiredJwtException e) {
                // 토큰 만료 — 클라이언트가 refresh 해야 함
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                    "{\"code\":5002,\"message\":\"토큰이 만료되었습니다\",\"timestamp\":\"" +
                    LocalDateTime.now() + "\"}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

### 5.5 JwtAuthenticationEntryPoint — 인증 실패 처리

인증되지 않은 요청에 대한 응답을 처리한다.

```java
package com.aroon.business.common.security;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
            "{\"code\":401,\"message\":\"인증이 필요합니다\",\"timestamp\":\"" +
            LocalDateTime.now() + "\"}"
        );
    }
}
```

### 5.6 JwtAccessDeniedHandler — 권한 부족 처리

인증은 되었지만 권한이 없는 요청에 대한 응답을 처리한다.

```java
package com.aroon.business.common.security;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
            "{\"code\":403,\"message\":\"접근 권한이 없습니다\",\"timestamp\":\"" +
            LocalDateTime.now() + "\"}"
        );
    }
}
```

### 5.7 SecurityConfig — Spring Security 설정

```java
package com.aroon.business.common.config;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // @PreAuthorize 사용을 위해
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (JWT 사용하므로 불필요)
            .csrf(csrf -> csrf.disable())

            // 세션 사용 안 함 (Stateless)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 예외 처리
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            )

            // URL 별 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 인증 없이 접근 가능한 엔드포인트
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/refresh"
                ).permitAll()

                // Swagger 등 개발 도구 (필요 시)
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()

                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )

            // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    /**
     * BCrypt 비밀번호 인코더
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager 빈 등록
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig
    ) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
```

### 5.8 AuthController — 인증 API 엔드포인트

```java
package com.aroon.business.controller;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 로그인 (토큰 발급)
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return ApiResponse.success(tokenResponse);
    }

    /**
     * 토큰 갱신 (Refresh Token으로 새 Access Token 발급)
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse tokenResponse = authService.refresh(request);
        return ApiResponse.success(tokenResponse);
    }

    /**
     * 내 정보 조회 (로그인한 사용자)
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponse userResponse = authService.getMyInfo(userDetails.getUserSeq());
        return ApiResponse.success(userResponse);
    }
}
```

### 5.9 AuthService — 인증 비즈니스 로직

```java
package com.aroon.business.service;

public interface AuthService {
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(RefreshTokenRequest request);
    UserResponse getMyInfo(Long userSeq);
}
```

```java
package com.aroon.business.service.impl;

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
        if (user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if ("Y".equals(user.getLoginLockYn())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
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
                .expiresIn(1800)  // 30분 (초 단위)
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
```

### 5.10 DTO 클래스

#### LoginRequest
```java
package com.aroon.business.dto.request;

@Data
public class LoginRequest {

    @NotBlank(message = "아이디를 입력해주세요")
    private String userId;

    @NotBlank(message = "비밀번호를 입력해주세요")
    private String password;
}
```

#### RefreshTokenRequest
```java
package com.aroon.business.dto.request;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh Token을 입력해주세요")
    private String refreshToken;
}
```

#### TokenResponse
```java
package com.aroon.business.dto.response;

@Data
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;    // "Bearer"
    private long expiresIn;      // Access Token 만료 시간 (초 단위)
}
```

---

## 6. 기존 코드 수정사항

### 6.1 UserServiceImpl — 비밀번호 BCrypt 암호화 적용

```java
// 변경 전
.password(request.getPassword())

// 변경 후
.password(passwordEncoder.encode(request.getPassword()))
```

`UserServiceImpl`에 `PasswordEncoder`를 주입하고, 사용자 생성 및 비밀번호 변경 시 암호화를 적용한다.

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;  // 추가

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // ... 중복 검사 동일 ...

        User user = User.builder()
                .userId(request.getUserId())
                .password(passwordEncoder.encode(request.getPassword()))  // BCrypt 암호화
                .email(request.getEmail())
                .nickname(request.getNickname())
                .status(1)
                .build();

        userMapper.insert(user);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userSeq, UserUpdateRequest request) {
        // ... 기존 로직 동일 ...

        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));  // BCrypt 암호화
        }

        // ... 나머지 동일 ...
    }
}
```

### 6.2 WebConfig — CORS 설정에 Authorization 헤더 허용

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("Authorization")    // 추가: 클라이언트에서 Authorization 헤더 접근 허용
            .maxAge(3600);
}
```

> **참고**: Spring Security를 도입하면 CORS 설정을 `SecurityConfig`에서도 처리해야 할 수 있다. `WebConfig`의 CORS와 충돌하지 않도록 `SecurityConfig`에서 `.cors(Customizer.withDefaults())`를 설정한다.

### 6.3 GlobalExceptionHandler — 인증 관련 에러 HTTP Status 매핑

현재 `resolveHttpStatus`의 5000번대 에러가 HTTP 200으로 응답된다. 인증 관련 에러는 적절한 HTTP Status로 매핑해야 한다.

```java
private HttpStatus resolveHttpStatus(ErrorCode errorCode) {
    return switch (errorCode.getCode()) {
        case 400 -> HttpStatus.BAD_REQUEST;
        case 401 -> HttpStatus.UNAUTHORIZED;
        case 403 -> HttpStatus.FORBIDDEN;
        case 404 -> HttpStatus.NOT_FOUND;
        default -> {
            // 인증 관련 에러 (5xxx)는 적절한 HTTP Status로 매핑
            if (errorCode.getCode() >= 5000 && errorCode.getCode() < 6000) {
                yield HttpStatus.UNAUTHORIZED;
            }
            // 1000번대 이상의 비즈니스 에러는 HTTP 200으로 응답
            if (errorCode.getCode() >= 1000) {
                yield HttpStatus.OK;
            }
            yield HttpStatus.INTERNAL_SERVER_ERROR;
        }
    };
}
```

### 6.4 i18n 메시지 추가

`src/main/resources/i18n/messages.properties` (또는 각 언어별 파일)에 인증 관련 메시지를 추가한다.

```properties
# 인증 (5xxx)
auth.invalid.credentials=아이디 또는 비밀번호가 올바르지 않습니다
auth.token.expired=토큰이 만료되었습니다
auth.token.invalid=유효하지 않은 토큰입니다
```

```properties
# messages_en.properties
auth.invalid.credentials=Invalid username or password
auth.token.expired=Token has expired
auth.token.invalid=Invalid token
```

---

## 7. API 명세

### 7.1 로그인

```
POST /api/auth/login
Content-Type: application/json
```

**Request Body:**
```json
{
    "userId": "admin",
    "password": "admin123"
}
```

**Response (성공):**
```json
{
    "code": 200,
    "message": "Success",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
        "tokenType": "Bearer",
        "expiresIn": 1800
    },
    "timestamp": "2026-02-16 14:30:00"
}
```

**Response (실패 — 잘못된 인증 정보):**
```json
{
    "code": 5001,
    "message": "아이디 또는 비밀번호가 올바르지 않습니다",
    "timestamp": "2026-02-16 14:30:00"
}
```

### 7.2 토큰 갱신

```
POST /api/auth/refresh
Content-Type: application/json
```

**Request Body:**
```json
{
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response (성공):**
```json
{
    "code": 200,
    "message": "Success",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiJ9...(새 토큰)",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...(새 토큰)",
        "tokenType": "Bearer",
        "expiresIn": 1800
    },
    "timestamp": "2026-02-16 14:30:00"
}
```

### 7.3 내 정보 조회

```
GET /api/auth/me
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Response (성공):**
```json
{
    "code": 200,
    "message": "Success",
    "data": {
        "userSeq": 1,
        "userId": "admin",
        "email": "admin@aroon.com",
        "nickname": "관리자",
        "status": 1,
        "createdAt": "2026-02-16 10:00:00",
        "updatedAt": "2026-02-16 14:30:00"
    },
    "timestamp": "2026-02-16 14:30:00"
}
```

### 7.4 인증이 필요한 API 호출 예시

```
GET /api/users
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**토큰 없이 호출 시:**
```json
{
    "code": 401,
    "message": "인증이 필요합니다",
    "timestamp": "2026-02-16 14:30:00"
}
```

---

## 8. 파일 구조

### 새로 생성하는 파일

```
src/main/java/com/aroon/business/
├── common/
│   ├── config/
│   │   ├── JwtProperties.java          ← JWT 설정값 바인딩
│   │   └── SecurityConfig.java         ← Spring Security 설정
│   └── security/
│       ├── JwtTokenProvider.java        ← 토큰 생성/검증
│       ├── JwtAuthenticationFilter.java ← 요청별 토큰 검증 필터
│       ├── JwtAuthenticationEntryPoint.java  ← 401 응답 처리
│       ├── JwtAccessDeniedHandler.java  ← 403 응답 처리
│       ├── CustomUserDetailsService.java ← DB 사용자 조회
│       └── CustomUserDetails.java       ← UserDetails 구현체
├── controller/
│   └── AuthController.java             ← 인증 API 컨트롤러
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java           ← 로그인 요청 DTO
│   │   └── RefreshTokenRequest.java    ← 토큰 갱신 요청 DTO
│   └── response/
│       └── TokenResponse.java          ← 토큰 응답 DTO
└── service/
    ├── AuthService.java                 ← 인증 서비스 인터페이스
    └── impl/
        └── AuthServiceImpl.java         ← 인증 서비스 구현
```

### 수정하는 파일

```
pom.xml                                  ← 의존성 추가
application.properties                   ← JWT 설정 추가
UserServiceImpl.java                     ← BCrypt 암호화 적용
WebConfig.java                           ← CORS exposedHeaders 추가
GlobalExceptionHandler.java              ← 5xxx 에러 HTTP Status 매핑
messages.properties (각 언어)             ← 인증 메시지 추가
```

---

## 9. 구현 순서

### Step 1: 의존성 추가
- `pom.xml`에 Spring Security, JJWT 의존성 추가
- `application.properties`에 JWT 설정 추가

### Step 2: JWT 핵심 컴포넌트
- `JwtProperties` 생성
- `JwtTokenProvider` 생성 (토큰 생성/검증)

### Step 3: Spring Security 설정
- `CustomUserDetails` 생성
- `CustomUserDetailsService` 생성
- `JwtAuthenticationFilter` 생성
- `JwtAuthenticationEntryPoint` 생성
- `JwtAccessDeniedHandler` 생성
- `SecurityConfig` 생성

### Step 4: 인증 API
- `LoginRequest`, `RefreshTokenRequest`, `TokenResponse` DTO 생성
- `AuthService` 인터페이스 생성
- `AuthServiceImpl` 구현
- `AuthController` 생성

### Step 5: 기존 코드 수정
- `UserServiceImpl`에 BCrypt 암호화 적용
- `WebConfig` CORS 설정 수정
- `GlobalExceptionHandler` 에러 매핑 수정
- i18n 메시지 파일에 인증 메시지 추가

### Step 6: 기존 비밀번호 마이그레이션
- DB에 저장된 기존 평문 비밀번호를 BCrypt로 변환
- 또는 schema.sql의 초기 데이터가 이미 BCrypt 해시이므로, 새로 DB를 초기화

---

## 10. 테스트 가이드

### 10.1 로그인 테스트

```bash
# 로그인 (토큰 발급)
curl -X POST http://localhost:8888/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userId": "admin", "password": "admin123"}'
```

### 10.2 인증된 API 호출 테스트

```bash
# 위 응답에서 받은 accessToken을 사용
curl -X GET http://localhost:8888/api/users \
  -H "Authorization: Bearer {accessToken}"
```

### 10.3 토큰 갱신 테스트

```bash
# refreshToken으로 새 토큰 발급
curl -X POST http://localhost:8888/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "{refreshToken}"}'
```

### 10.4 인증 실패 테스트

```bash
# 토큰 없이 호출 → 401
curl -X GET http://localhost:8888/api/users

# 잘못된 토큰 → 401
curl -X GET http://localhost:8888/api/users \
  -H "Authorization: Bearer invalid-token"

# 잘못된 비밀번호 → 5001
curl -X POST http://localhost:8888/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userId": "admin", "password": "wrongpassword"}'
```

### 10.5 내 정보 조회 테스트

```bash
curl -X GET http://localhost:8888/api/auth/me \
  -H "Authorization: Bearer {accessToken}"
```

---

## JWT 토큰 구조 참고

### Access Token Payload 예시
```json
{
  "sub": "admin",
  "userSeq": 1,
  "roles": ["ROLE_ADMIN"],
  "type": "access",
  "iat": 1739692200,
  "exp": 1739694000
}
```

### Refresh Token Payload 예시
```json
{
  "sub": "admin",
  "type": "refresh",
  "iat": 1739692200,
  "exp": 1740297000
}
```
