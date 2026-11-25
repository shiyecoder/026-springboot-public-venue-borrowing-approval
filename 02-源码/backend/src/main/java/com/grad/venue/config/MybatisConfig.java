package com.grad.venue.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisConfig {

    /** 服务端分页插件，所有列表接口都走物理分页 */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor i = new MybatisPlusInterceptor();
        PaginationInnerInterceptor p = new PaginationInnerInterceptor(DbType.MYSQL);
        p.setMaxLimit(200L);
        i.addInnerInterceptor(p);
        return i;
    }
}
