package com.codechronicles.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 扩展配置，集中注册接口拦截器。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthRequiredInterceptor authRequiredInterceptor;

    public WebMvcConfig(AuthRequiredInterceptor authRequiredInterceptor) {
        this.authRequiredInterceptor = authRequiredInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authRequiredInterceptor)
                .addPathPatterns("/api/**");
    }
}
