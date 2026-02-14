package com.aroon.business.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class MybatisPlusConfig {

    /**
     * 페이징 플러그인
     *
     * 이 설정이 있어야 MyBatis-Plus의 Page 객체를 사용한 페이징이 동작합니다.
     * selectPage() 호출 시 자동으로 COUNT 쿼리 + LIMIT/OFFSET이 추가됩니다.
     *
     * 사용 예시 (Phase 2에서):
     *   Page<User> page = new Page<>(1, 10);  // 1페이지, 10건
     *   userMapper.selectPage(page, null);
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 자동 채움 핸들러
     *
     * Entity의 @TableField(fill = FieldFill.INSERT) 또는 @TableField(fill = FieldFill.INSERT_UPDATE)
     * 어노테이션이 붙은 필드에 자동으로 값을 채워줍니다.
     *
     * INSERT 시: createdAt, updatedAt 자동 설정
     * UPDATE 시: updatedAt만 자동 갱신
     *
     * Entity에서의 사용 (Phase 2에서):
     *   @TableField(fill = FieldFill.INSERT)
     *   private LocalDateTime createdAt;
     *
     *   @TableField(fill = FieldFill.INSERT_UPDATE)
     *   private LocalDateTime updatedAt;
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime::now, LocalDateTime.class);
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime::now, LocalDateTime.class);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime::now, LocalDateTime.class);
            }
        };
    }

}
