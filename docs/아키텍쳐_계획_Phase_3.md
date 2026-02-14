# Phase 3: 인증/인가 상세 가이드

## 개요

Phase 2에서 완성한 CRUD API에 **JWT 기반 인증**과 **권한(Permission) 기반 인가**를 적용합니다.
로그인하지 않으면 API를 호출할 수 없고, 로그인해도 해당 권한이 없으면 접근이 차단됩니다.

### 전제 조건
- Phase 1, Phase 2의 모든 Step이 완료되어 있어야 합니다
- DB에 `user_role`, `role_permission` 관계 데이터가 존재해야 합니다 (schema.sql 초기 데이터)

### Phase 3 작업 목록

| 순서 | 작업 | 생성/수정 파일 | 설명 |
|:----:|------|---------------|------|
| 3-1 | 의존성 추가 | `pom.xml` | Spring Security + JWT 라이브러리 |
| 3-2 | JWT 설정 추가 | `application.properties` | secret, 만료시간 설정 |
| 3-3 | JwtTokenProvider | 신규 | JWT 토큰 생성/검증/파싱 |
| 3-4 | SecurityUserDetails | 신규 | Spring Security 사용자 상세 정보 |
| 3-5 | UserDetailsServiceImpl | 신규 | DB에서 사용자+역할+권한 로딩 |
| 3-6 | JwtAuthenticationFilter | 신규 | 매 요청마다 토큰 검증 필터 |
| 3-7 | SecurityConfig | 신규 | 보안 필터 체인, 경로별 인증/인가 규칙 |
| 3-8 | Auth DTO | 신규 2개 | LoginRequest, LoginResponse |
| 3-9 | AuthService / AuthServiceImpl | 신규 2개 | 로그인, 토큰 갱신 비즈니스 로직 |
| 3-10 | AuthController | 신규 | 인증 API 엔드포인트 |
| 3-11 | UserServiceImpl 수정 | 수정 | BCrypt 비밀번호 암호화 적용 |
| 3-12 | Controller 권한 적용 | 수정 4개 | `@PreAuthorize` 권한 검증 추가 |
| 3-13 | XML 매퍼 (권한 조회) | 수정 1개 | 사용자별 권한 조인 쿼리 |

### Phase 3 완료 후 디렉토리 구조 (신규/수정 파일만 표시)

```
src/main/java/com/aroon/business/
│
├── common/config/
│   └── SecurityConfig.java                    (신규)
│
├── security/                                  ← 신규 패키지
│   ├── jwt/
│   │   ├── JwtTokenProvider.java              (신규)
│   │   └── JwtAuthenticationFilter.java       (신규)
│   ├── SecurityUserDetails.java               (신규)
│   └── UserDetailsServiceImpl.java            (신규)
│
├── controller/
│   ├── AuthController.java                    (신규)
│   ├── UserController.java                    (수정 - @PreAuthorize 추가)
│   ├── RoleController.java                    (수정)
│   ├── PermissionController.java              (수정)
│   └── MenuController.java                    (수정)
│
├── dto/
│   ├── request/
│   │   └── LoginRequest.java                  (신규)
│   └── response/
│       └── LoginResponse.java                 (신규)
│
├── service/
│   ├── AuthService.java                       (신규)
│   └── impl/
│       ├── AuthServiceImpl.java               (신규)
│       └── UserServiceImpl.java               (수정 - BCrypt 적용)
│
├── mapper/
│   └── UserMapper.java                        (수정 - 권한 조회 메서드 추가)

src/main/resources/
├── application.properties                     (수정 - JWT 설정 추가)
└── mapper/
    └── UserMapper.xml                         (수정 - 권한 조인 쿼리 추가)
```

---

## 전체 인증/인가 흐름 이해

코드 작성 전에 전체 흐름을 먼저 이해하세요.

### 로그인 흐름

```
Client                    Server
  │                         │
  │  POST /api/auth/login   │
  │  {username, password}   │
  │────────────────────────▶│
  │                         │
  │                    ┌────┴────────────────────────────┐
  │                    │ AuthController.login()           │
  │                    │   └→ AuthService.login()         │
  │                    │       ├→ UserMapper: username 조회│
  │                    │       ├→ BCrypt: 비밀번호 검증     │
  │                    │       ├→ UserMapper: 권한 목록 조회│
  │                    │       └→ JwtTokenProvider:        │
  │                    │          accessToken 생성         │
  │                    │          refreshToken 생성        │
  │                    └────┬────────────────────────────┘
  │                         │
  │  {accessToken,          │
  │   refreshToken}         │
  │◀────────────────────────│
```

### 인증된 API 요청 흐름

```
Client                                                     Server
  │                                                          │
  │  GET /api/users                                          │
  │  Authorization: Bearer eyJhbGciOi...                     │
  │─────────────────────────────────────────────────────────▶│
  │                                                          │
  │    ┌──────────────────────────────────────────────────────┤
  │    │ JwtAuthenticationFilter                              │
  │    │   ├→ Authorization 헤더에서 토큰 추출                  │
  │    │   ├→ JwtTokenProvider.validateToken() 검증            │
  │    │   ├→ JwtTokenProvider.getUsername() 추출              │
  │    │   ├→ UserDetailsService.loadUserByUsername()          │
  │    │   │     └→ DB에서 사용자 + 권한 조회                   │
  │    │   └→ SecurityContext에 인증 정보 저장                  │
  │    ├──────────────────────────────────────────────────────┤
  │    │ SecurityConfig                                       │
  │    │   └→ @PreAuthorize("hasAuthority('user:read')") 체크 │
  │    ├──────────────────────────────────────────────────────┤
  │    │ UserController.getUsers()                            │
  │    │   └→ 정상 처리                                        │
  │    └──────────────────────────────────────────────────────┤
  │                                                          │
  │  {"code":200, "data":[...]}                              │
  │◀─────────────────────────────────────────────────────────│
```

---

## Step 3-1. 의존성 추가 (pom.xml)

`<dependencies>` 블록 안에 아래 4개를 추가합니다.

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
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

추가 후:

```bash
./mvnw dependency:resolve
```

> **주의**: Spring Security를 추가하면 모든 API가 자동으로 인증 필수가 됩니다.
> SecurityConfig(Step 3-7)를 작성하기 전까지 API 호출이 막힐 수 있으니,
> Step 3-1~3-7까지 한번에 작성하는 것을 권장합니다.

---

## Step 3-2. application.properties JWT 설정 추가

기존 `application.properties` 파일 맨 아래에 추가합니다.

```properties
# ── JWT ──
jwt.secret=YXJvb24tand0LXNlY3JldC1rZXktZm9yLWhtYWMtc2hhMjU2LW11c3QtYmUtbG9uZy1lbm91Z2g=
jwt.access-token-expiration=3600000
jwt.refresh-token-expiration=604800000
```

| 설정 | 값 | 설명 |
|------|------|------|
| `jwt.secret` | Base64 인코딩된 키 | HMAC-SHA256 서명 키 (최소 256bit) |
| `jwt.access-token-expiration` | 3600000 | Access Token 만료: **1시간** (밀리초) |
| `jwt.refresh-token-expiration` | 604800000 | Refresh Token 만료: **7일** (밀리초) |

> **운영 주의**: `jwt.secret`은 반드시 환경변수나 외부 설정으로 관리하세요.
> 위 값은 개발용 샘플입니다.

---

## Step 3-3. JwtTokenProvider (토큰 생성/검증)

### 파일 경로

```
src/main/java/com/aroon/business/security/jwt/JwtTokenProvider.java
```

### 역할

JWT 토큰의 생성, 검증, 클레임 추출을 담당하는 유틸리티 클래스입니다.

### 소스 코드

```java
package com.aroon.business.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretBase64;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(secretBase64);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // ──────────────────────────────────────
    // 토큰 생성
    // ──────────────────────────────────────

    /**
     * Access Token 생성
     *
     * @param username    사용자명
     * @param userSeq     사용자 SEQ
     * @param permissions 권한 목록 (예: ["user:read", "user:create", ...])
     */
    public String createAccessToken(String username, Long userSeq, List<String> permissions) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(username)
                .claim("userSeq", userSeq)
                .claim("permissions", permissions)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(String username, Long userSeq) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(username)
                .claim("userSeq", userSeq)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    // ──────────────────────────────────────
    // 토큰 검증
    // ──────────────────────────────────────

    /**
     * 토큰이 유효한지 검증
     *
     * @return true: 유효 / false: 만료 또는 위변조
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ──────────────────────────────────────
    // 클레임 추출
    // ──────────────────────────────────────

    /**
     * 토큰에서 username(subject) 추출
     */
    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 토큰에서 userSeq 추출
     */
    public Long getUserSeq(String token) {
        return getClaims(token).get("userSeq", Long.class);
    }

    /**
     * 토큰에서 권한 목록 추출
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissions(String token) {
        return getClaims(token).get("permissions", List.class);
    }

    /**
     * 토큰의 타입 추출 ("access" 또는 "refresh")
     */
    public String getTokenType(String token) {
        return getClaims(token).get("type", String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

### JWT 토큰 구조 설명

```
Access Token 예시 (디코딩된 payload):
{
  "sub": "admin",             ← username
  "userSeq": 1,               ← 사용자 SEQ
  "permissions": [             ← 권한 목록
    "user:read",
    "user:create",
    "role:read",
    ...
  ],
  "type": "access",           ← 토큰 타입
  "iat": 1739440800,          ← 발급 시각
  "exp": 1739444400           ← 만료 시각 (1시간 후)
}

Refresh Token 예시:
{
  "sub": "admin",
  "userSeq": 1,
  "type": "refresh",
  "iat": 1739440800,
  "exp": 1740045600           ← 만료 시각 (7일 후)
}
```

---

## Step 3-4. SecurityUserDetails (사용자 상세 정보)

### 파일 경로

```
src/main/java/com/aroon/business/security/SecurityUserDetails.java
```

### 역할

Spring Security의 `UserDetails` 인터페이스를 구현하여, 인증된 사용자 정보를 담는 객체입니다.
`SecurityContext`에 저장되어 요청 처리 중 어디서든 접근 가능합니다.

### 소스 코드

```java
package com.aroon.business.security;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Builder
public class SecurityUserDetails implements UserDetails {

    private final Long userSeq;
    private final String username;
    private final String password;
    private final Integer status;
    private final List<String> permissions;

    /**
     * 권한 목록 반환
     * permissions: ["user:read", "user:create", ...] → SimpleGrantedAuthority로 변환
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != null && status == 1;   // status=1이면 잠금 아님
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status != null && status == 1;   // status=1이면 활성
    }
}
```

---

## Step 3-5. UserDetailsServiceImpl (사용자 로딩)

### 파일 경로

```
src/main/java/com/aroon/business/security/UserDetailsServiceImpl.java
```

### 역할

`UserDetailsService` 인터페이스를 구현하여, username으로 DB에서 사용자 정보 + 권한 목록을 조회합니다.
JwtAuthenticationFilter에서 매 요청마다 호출됩니다.

### 소스 코드

```java
package com.aroon.business.security;

import com.aroon.business.entity.core.User;
import com.aroon.business.mapper.core.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. username으로 사용자 조회
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        // 2. 사용자의 권한 목록 조회 (user → user_role → role_permission → permission)
        List<String> permissions = userMapper.selectPermissionsByUserSeq(user.getUserSeq());

        // 3. SecurityUserDetails 생성
        return SecurityUserDetails.builder()
                .userSeq(user.getUserSeq())
                .username(user.getUsername())
                .password(user.getPassword())
                .status(user.getStatus())
                .permissions(permissions)
                .build();
    }
}
```

### 필요한 Mapper 메서드

`UserMapper.java`에 권한 조회 메서드를 추가해야 합니다. (Step 3-13에서 구현)

---

## Step 3-6. JwtAuthenticationFilter (인증 필터)

### 파일 경로

```
src/main/java/com/aroon/business/security/jwt/JwtAuthenticationFilter.java
```

### 역할

모든 HTTP 요청을 가로채서 JWT 토큰을 검증하고, 인증 정보를 SecurityContext에 저장합니다.

### 소스 코드

```java
package com.aroon.business.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Authorization 헤더에서 토큰 추출
        String token = resolveToken(request);

        // 2. 토큰이 있고 유효한 경우
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // access 토큰인지 확인
            String tokenType = jwtTokenProvider.getTokenType(token);
            if ("access".equals(tokenType)) {
                // 3. 토큰에서 username 추출
                String username = jwtTokenProvider.getUsername(token);

                // 4. DB에서 사용자 + 권한 로딩
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 5. SecurityContext에 인증 정보 저장
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 6. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * "Authorization: Bearer eyJhbGciOi..." 헤더에서 토큰 부분만 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

### 필터 동작 순서

```
HTTP 요청 도착
    │
    ▼
[JwtAuthenticationFilter]
    │
    ├── Authorization 헤더 없음?  →  그냥 통과 (SecurityContext 비어있음)
    │                                  └→ SecurityConfig에서 permitAll이면 → Controller 접근 가능
    │                                  └→ authenticated 필요하면 → 401 에러
    │
    └── Authorization 헤더 있음?
         ├── 토큰 유효?  →  UserDetailsService로 사용자 로딩
         │                  └→ SecurityContext에 인증 정보 저장
         │                  └→ @PreAuthorize 권한 체크 통과하면 → Controller 접근
         │
         └── 토큰 무효/만료?  →  그냥 통과 (SecurityContext 비어있음)
                                  └→ 401 에러
```

---

## Step 3-7. SecurityConfig (보안 설정)

### 파일 경로

```
src/main/java/com/aroon/business/common/config/SecurityConfig.java
```

### 역할

Spring Security의 핵심 설정입니다.
어떤 경로를 인증 없이 허용할지, JWT 필터를 어디에 삽입할지, CORS/CSRF 정책 등을 정의합니다.

### 소스 코드

```java
package com.aroon.business.common.config;

import com.aroon.business.common.response.ApiResponse;
import com.aroon.business.security.jwt.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 인증 없이 접근 가능한 경로 (White List)
     */
    private static final String[] WHITE_LIST = {
            "/api/auth/login",
            "/api/auth/refresh",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (REST API는 토큰 기반이므로 불필요)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 사용 안 함 (JWT 기반 = STATELESS)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 경로별 인증/인가 규칙
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST).permitAll()     // White List는 인증 불필요
                        .anyRequest().authenticated()                 // 나머지는 모두 인증 필요
                )

                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 삽입
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 인증 실패 시 (401) 커스텀 응답
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            ObjectMapper mapper = new ObjectMapper();
                            mapper.registerModule(new JavaTimeModule());
                            response.getWriter().write(
                                    mapper.writeValueAsString(ApiResponse.error(401, "인증이 필요합니다"))
                            );
                        })
                        // 인가 실패 시 (403) 커스텀 응답
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            ObjectMapper mapper = new ObjectMapper();
                            mapper.registerModule(new JavaTimeModule());
                            response.getWriter().write(
                                    mapper.writeValueAsString(ApiResponse.error(403, "접근 권한이 없습니다"))
                            );
                        })
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
     * AuthenticationManager (로그인 시 사용)
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS 설정
     * Phase 1의 WebConfig CORS 설정을 여기로 통합합니다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));              // 운영 시 실제 도메인으로 변경
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
```

### 핵심 포인트

| 설정 | 값 | 이유 |
|------|------|------|
| CSRF | 비활성화 | JWT 토큰 기반이므로 CSRF 보호 불필요 |
| Session | STATELESS | 서버에 세션 저장하지 않음 (JWT에 모든 정보 포함) |
| White List | `/api/auth/login`, `/api/auth/refresh` | 로그인/토큰갱신은 토큰 없이 접근 가능해야 함 |
| JWT 필터 | `UsernamePasswordAuthenticationFilter` 앞 | Spring Security 기본 인증 전에 JWT 검증 |
| 401 응답 | ApiResponse 형태 | 스프링 기본 에러 대신 우리 포맷으로 통일 |
| 403 응답 | ApiResponse 형태 | 권한 없을 때도 우리 포맷으로 통일 |

> **참고**: SecurityConfig에 CORS를 설정했으므로, Phase 1의 `WebConfig.java`에서
> `addCorsMappings()` 메서드 내용을 제거하거나 파일 자체를 삭제해도 됩니다.
> Security 레이어의 CORS가 우선 적용됩니다.

---

## Step 3-8. Auth DTO

### 3-8-A. LoginRequest.java

**파일 경로**: `src/main/java/com/aroon/business/dto/request/LoginRequest.java`

```java
package com.aroon.business.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "{validation.username.required}")
    private String username;

    @NotBlank(message = "{validation.password.required}")
    private String password;
}
```

### 3-8-B. LoginResponse.java

**파일 경로**: `src/main/java/com/aroon/business/dto/response/LoginResponse.java`

```java
package com.aroon.business.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private Long userSeq;
    private String username;
    private String nickname;
}
```

---

## Step 3-9. AuthService / AuthServiceImpl

### 3-9-A. AuthService.java

**파일 경로**: `src/main/java/com/aroon/business/service/AuthService.java`

```java
package com.aroon.business.service;

import com.aroon.business.dto.request.LoginRequest;
import com.aroon.business.dto.response.LoginResponse;
import com.aroon.business.dto.response.UserResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse refreshToken(String refreshToken);

    UserResponse getCurrentUser(Long userSeq);
}
```

### 3-9-B. AuthServiceImpl.java

**파일 경로**: `src/main/java/com/aroon/business/service/impl/AuthServiceImpl.java`

```java
package com.aroon.business.service.impl;

import com.aroon.business.common.exception.BusinessException;
import com.aroon.business.common.exception.ErrorCode;
import com.aroon.business.dto.request.LoginRequest;
import com.aroon.business.dto.response.LoginResponse;
import com.aroon.business.dto.response.UserResponse;
import com.aroon.business.entity.core.User;
import com.aroon.business.security.jwt.JwtTokenProvider;
import com.aroon.business.service.AuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final com.aroon.business.mapper.core.UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // 1. username으로 사용자 조회
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(com.aroon.business.entity.core.User::getUsername, request.getUsername())
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 2. 비밀번호 검증 (BCrypt)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 3. 계정 활성 상태 확인
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 4. 사용자 권한 목록 조회
        List<String> permissions = userMapper.selectPermissionsByUserSeq(user.getUserSeq());

        // 5. 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getUsername(), user.getUserSeq(), permissions
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(
                user.getUsername(), user.getUserSeq()
        );

        // 6. 응답 반환
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userSeq(user.getUserSeq())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse refreshToken(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 2. 토큰 타입 확인
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 3. 사용자 정보 추출
        String username = jwtTokenProvider.getUsername(refreshToken);
        Long userSeq = jwtTokenProvider.getUserSeq(refreshToken);

        // 4. 사용자 존재 여부 확인
        com.aroon.business.entity.core.User user = userMapper.selectById(userSeq);
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 5. 권한 재조회 (권한이 변경되었을 수 있으므로)
        List<String> permissions = userMapper.selectPermissionsByUserSeq(userSeq);

        // 6. 새 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(username, userSeq, permissions);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(username, userSeq);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userSeq(user.getUserSeq())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userSeq) {
        com.aroon.business.entity.core.User user = userMapper.selectById(userSeq);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return UserResponse.builder()
                .userSeq(user.getUserSeq())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
```

---

## Step 3-10. AuthController

### 파일 경로

```
src/main/java/com/aroon/business/controller/AuthController.java
```

### 소스 코드

```java
package com.aroon.business.controller;

import com.aroon.business.common.response.ApiResponse;
import com.aroon.business.dto.request.LoginRequest;
import com.aroon.business.dto.response.LoginResponse;
import com.aroon.business.dto.response.UserResponse;
import com.aroon.business.security.SecurityUserDetails;
import com.aroon.business.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 로그인
     * POST /api/auth/login
     * 인증 불필요 (White List)
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 토큰 갱신
     * POST /api/auth/refresh
     * 인증 불필요 (White List) - refreshToken을 body로 받음
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        return ApiResponse.success(authService.refreshToken(refreshToken));
    }

    /**
     * 현재 로그인 사용자 정보
     * GET /api/auth/me
     * 인증 필요
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal SecurityUserDetails userDetails) {
        return ApiResponse.success(authService.getCurrentUser(userDetails.getUserSeq()));
    }
}
```

### `@AuthenticationPrincipal` 설명

```
SecurityContext에 저장된 SecurityUserDetails를 자동으로 주입받습니다.
JwtAuthenticationFilter에서 인증에 성공하면 SecurityContext에 저장되므로,
인증된 사용자의 정보를 Controller에서 바로 사용할 수 있습니다.

사용 예시:
  @AuthenticationPrincipal SecurityUserDetails userDetails
  userDetails.getUserSeq()      → 1
  userDetails.getUsername()     → "admin"
  userDetails.getPermissions()  → ["user:read", "user:create", ...]
```

---

## Step 3-11. UserServiceImpl 수정 (BCrypt 적용)

### 파일 경로

```
src/main/java/com/aroon/business/service/impl/UserServiceImpl.java
```

### 변경 내용

Phase 2에서 `// Phase 3에서 BCrypt 적용`으로 표시한 부분을 수정합니다.

**1. 필드 추가** — 클래스 상단 `private final` 영역에 추가:

```java
private final UserMapper userMapper;
private final PasswordEncoder passwordEncoder;   // ← 추가
```

**2. createUser 메서드** — password 세팅 부분 변경:

```java
// 변경 전
.password(request.getPassword())

// 변경 후
.password(passwordEncoder.encode(request.getPassword()))
```

**3. updateUser 메서드** — password 업데이트 부분 변경:

```java
// 변경 전
if (request.getPassword() != null) {
    user.setPassword(request.getPassword());
}

// 변경 후
if (request.getPassword() != null) {
    user.setPassword(passwordEncoder.encode(request.getPassword()));
}
```

> import 추가: `import org.springframework.security.crypto.password.PasswordEncoder;`

---

## Step 3-12. Controller 권한 적용 (@PreAuthorize)

Phase 2에서 만든 4개 Controller에 `@PreAuthorize` 어노테이션을 추가합니다.
ARCHITECTURE.md의 API 설계에 정의된 권한을 그대로 적용합니다.

### 3-12-A. UserController.java — 변경 부분

```java
import org.springframework.security.access.prepost.PreAuthorize;

// 각 메서드에 추가:

@GetMapping
@PreAuthorize("hasAuthority('user:read')")
public ApiResponse<List<UserResponse>> getUsers() { ... }

@GetMapping("/{userSeq}")
@PreAuthorize("hasAuthority('user:read')")
public ApiResponse<UserResponse> getUser(...) { ... }

@PostMapping
@PreAuthorize("hasAuthority('user:create')")
public ApiResponse<UserResponse> createUser(...) { ... }

@PutMapping("/{userSeq}")
@PreAuthorize("hasAuthority('user:update')")
public ApiResponse<UserResponse> updateUser(...) { ... }

@DeleteMapping("/{userSeq}")
@PreAuthorize("hasAuthority('user:delete')")
public ApiResponse<Void> deleteUser(...) { ... }
```

### 3-12-B. RoleController.java — 변경 부분

```java
@GetMapping          → @PreAuthorize("hasAuthority('role:read')")
@GetMapping("/{..}") → @PreAuthorize("hasAuthority('role:read')")
@PostMapping         → @PreAuthorize("hasAuthority('role:create')")
@PutMapping("/{..}") → @PreAuthorize("hasAuthority('role:update')")
@DeleteMapping       → @PreAuthorize("hasAuthority('role:delete')")
```

### 3-12-C. PermissionController.java — 변경 부분

```java
@GetMapping          → @PreAuthorize("hasAuthority('permission:read')")
@GetMapping("/{..}") → @PreAuthorize("hasAuthority('permission:read')")
@PostMapping         → @PreAuthorize("hasAuthority('permission:create')")
@PutMapping("/{..}") → @PreAuthorize("hasAuthority('permission:update')")
@DeleteMapping       → @PreAuthorize("hasAuthority('permission:delete')")
```

### 3-12-D. MenuController.java — 변경 부분

```java
@GetMapping          → @PreAuthorize("hasAuthority('menu:read')")
@GetMapping("/list") → @PreAuthorize("hasAuthority('menu:read')")
@GetMapping("/{..}") → @PreAuthorize("hasAuthority('menu:read')")
@PostMapping         → @PreAuthorize("hasAuthority('menu:create')")
@PutMapping("/{..}") → @PreAuthorize("hasAuthority('menu:update')")
@DeleteMapping       → @PreAuthorize("hasAuthority('menu:delete')")
```

### 권한 검증 흐름

```
@PreAuthorize("hasAuthority('user:read')")

  1. SecurityContext에서 현재 인증 정보를 가져옴
  2. SecurityUserDetails.getAuthorities() 호출
  3. authorities 목록에 'user:read'가 있는지 확인
  4. 있으면 → Controller 메서드 실행
  5. 없으면 → 403 Forbidden (SecurityConfig의 accessDeniedHandler 응답)
```

---

## Step 3-13. UserMapper 수정 + XML 매퍼

### 3-13-A. UserMapper.java — 메서드 추가

**파일 경로**: `src/main/java/com/aroon/business/mapper/UserMapper.java`

기존 내용을 유지하면서 메서드를 추가합니다.

```java
package com.aroon.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<com.aroon.business.entity.core.User> {

    /**
     * 사용자의 권한(permission) 코드 목록을 조회합니다.
     * 조인 경로: user_role → role_permission → permission
     *
     * @param userSeq 사용자 SEQ
     * @return 권한 코드 목록 (예: ["user:read", "user:create", "role:read", ...])
     */
    List<String> selectPermissionsByUserSeq(@Param("userSeq") Long userSeq);
}
```

### 3-13-B. UserMapper.xml — 조인 쿼리 작성

**파일 경로**: `src/main/resources/mapper/UserMapper.xml`

Phase 2에서 빈 파일로 만들어 둔 XML을 아래 내용으로 교체합니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.aroon.business.mapper.core.UserMapper">

    <!--
        사용자의 권한(permission) 코드 목록 조회
        조인 경로: user_role → role_permission → permission
        결과 예: ["user:read", "user:create", "role:read", ...]
    -->
    <select id="selectPermissionsByUserSeq" resultType="java.lang.String">
        SELECT DISTINCT p.permission_code
        FROM user_role ur
        INNER JOIN role_permission rp ON ur.role_seq = rp.role_seq
        INNER JOIN permission p ON rp.permission_seq = p.permission_seq
        WHERE ur.user_seq = #{userSeq}
    </select>

</mapper>
```

### SQL 실행 흐름 설명

```
user_seq = 1 (admin) 으로 조회하면:

user_role:       user_seq=1, role_seq=1  (admin → ROLE_ADMIN)
                       │
                       ▼
role_permission: role_seq=1, permission_seq=1   (ROLE_ADMIN → user:read)
                 role_seq=1, permission_seq=2   (ROLE_ADMIN → user:create)
                 role_seq=1, permission_seq=3   (ROLE_ADMIN → user:update)
                 ...                            (ROLE_ADMIN은 전체 16개 권한)
                       │
                       ▼
permission:      permission_code = "user:read", "user:create", ...

결과: ["user:read", "user:create", "user:update", "user:delete",
       "role:read", "role:create", ..., "menu:delete"]
       → 총 16개 권한 코드
```

---

## API 테스트 가이드

### 1. 로그인

```bash
# 관리자 로그인 (admin / admin123)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

기대 응답:
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "userSeq": 1,
    "username": "admin",
    "nickname": "관리자"
  },
  "timestamp": "2026-02-13T10:00:00"
}
```

### 2. 인증된 API 호출

```bash
# accessToken을 변수에 저장
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

# 사용자 목록 조회 (인증 필요)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users

# 현재 로그인 사용자 정보
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/auth/me
```

### 3. 인증 없이 호출 (401 에러)

```bash
# Authorization 헤더 없이 호출
curl http://localhost:8080/api/users
```

기대 응답:
```json
{"code": 401, "message": "인증이 필요합니다", "timestamp": "..."}
```

### 4. 권한 없이 호출 (403 에러)

```bash
# ROLE_USER (조회 권한만 있음) 사용자로 로그인 후 삭제 시도
curl -X DELETE -H "Authorization: Bearer $USER_TOKEN" http://localhost:8080/api/users/2
```

기대 응답:
```json
{"code": 403, "message": "접근 권한이 없습니다", "timestamp": "..."}
```

### 5. 토큰 갱신

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"eyJhbGciOiJIUzI1NiJ9..."}'
```

### 6. 다국어 에러 메시지

```bash
# 영어로 로그인 실패 메시지
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "Accept-Language: en" \
  -d '{"username":"admin","password":"wrongpassword"}'
```

기대 응답:
```json
{"code": 5001, "message": "Invalid username or password", "timestamp": "..."}
```

---

## 체크리스트

### 파일 생성/수정 체크

| # | 파일 | 상태 | 확인 |
|---|------|------|------|
| 1 | `pom.xml` — Security + JWT 의존성 | 수정 | ☐ |
| 2 | `application.properties` — JWT 설정 | 수정 | ☐ |
| 3 | `security/jwt/JwtTokenProvider.java` | 신규 | ☐ |
| 4 | `security/SecurityUserDetails.java` | 신규 | ☐ |
| 5 | `security/UserDetailsServiceImpl.java` | 신규 | ☐ |
| 6 | `security/jwt/JwtAuthenticationFilter.java` | 신규 | ☐ |
| 7 | `common/config/SecurityConfig.java` | 신규 | ☐ |
| 8 | `dto/request/LoginRequest.java` | 신규 | ☐ |
| 9 | `dto/response/LoginResponse.java` | 신규 | ☐ |
| 10 | `service/AuthService.java` | 신규 | ☐ |
| 11 | `service/impl/AuthServiceImpl.java` | 신규 | ☐ |
| 12 | `controller/AuthController.java` | 신규 | ☐ |
| 13 | `service/impl/UserServiceImpl.java` — BCrypt 적용 | 수정 | ☐ |
| 14 | `controller/UserController.java` — @PreAuthorize | 수정 | ☐ |
| 15 | `controller/RoleController.java` — @PreAuthorize | 수정 | ☐ |
| 16 | `controller/PermissionController.java` — @PreAuthorize | 수정 | ☐ |
| 17 | `controller/MenuController.java` — @PreAuthorize | 수정 | ☐ |
| 18 | `mapper/UserMapper.java` — 메서드 추가 | 수정 | ☐ |
| 19 | `resources/mapper/UserMapper.xml` — 조인 쿼리 | 수정 | ☐ |
| 20 | `common/config/WebConfig.java` — CORS 제거 (선택) | 수정/삭제 | ☐ |

### 빌드 및 기동 확인

```bash
# 컴파일 확인
./mvnw compile -DskipTests

# 서버 기동
./mvnw spring-boot:run
```

### 동작 확인 시나리오

```
1. 로그인 없이 API 호출        → 401 응답 확인      ☐
2. 로그인 (admin/admin123)     → 토큰 발급 확인      ☐
3. 토큰으로 GET /api/users     → 200 응답 확인       ☐
4. 토큰으로 GET /api/auth/me   → 내 정보 반환 확인    ☐
5. 만료/잘못된 토큰으로 호출     → 401 응답 확인       ☐
6. refreshToken으로 갱신       → 새 토큰 발급 확인    ☐
7. 권한 없는 API 호출          → 403 응답 확인       ☐
```

---

## 다음 단계: Phase 4 미리보기

Phase 3이 완료되면 Phase 4에서 관계 할당과 고급 기능을 구현합니다.

| 작업 | 설명 |
|------|------|
| 사용자-역할 할당 API | `PUT /api/users/{userSeq}/roles` |
| 역할-권한 할당 API | `PUT /api/roles/{roleSeq}/permissions` |
| 역할-메뉴 할당 API | `PUT /api/roles/{roleSeq}/menus` |
| 사용자별 메뉴 조회 | `GET /api/menus/user` — 로그인 사용자의 메뉴만 반환 |
| 다국어 동적 콘텐츠 | TranslationHelper를 활용한 메뉴명/역할명 번역 |
| 페이징 처리 | MyBatis-Plus Page 활용한 목록 조회 |
