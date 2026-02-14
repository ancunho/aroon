package com.aroon.business.common.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration
public class MessageConfig {

    /**
     * 다국어 메시지 소스 설정
     *
     * src/main/resources/i18n/ 디렉토리에서 메시지 파일을 로딩합니다.
     *   - messages.properties      → 기본 (한국어)
     *   - messages_en.properties   → 영어
     *   - messages_zh.properties   → 중국어
     *
     * 사용 방법:
     *   messageSource.getMessage("user.not.found", null, locale)
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setCacheSeconds(60);  // 60초마다 파일 변경 체크
        return source;
    }

    /**
     * 로케일 결정 전략
     *
     * 클라이언트의 Accept-Language 헤더를 읽어 Locale을 결정합니다.
     *   - Accept-Language: ko  → 한국어
     *   - Accept-Language: en  → 영어
     *   - Accept-Language: zh  → 중국어
     *   - 헤더 없음            → 기본값 한국어
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.KOREAN);
        resolver.setSupportedLocales(List.of(
                Locale.KOREAN,
                Locale.ENGLISH,
                Locale.CHINESE
        ));
        return resolver;
    }

}
