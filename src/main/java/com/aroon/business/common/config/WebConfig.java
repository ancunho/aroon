package com.aroon.business.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * CORS 설정
     *
     * 프론트엔드 개발 시 localhost:3000 등에서 API 호출이 가능하도록 허용합니다.
     * 운영 배포 시에는 allowedOrigins를 실제 도메인으로 제한하세요.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")            // 운영 시 실제 도메인으로 변경
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .maxAge(3600);                   // preflight 캐시 1시간
    }

}
