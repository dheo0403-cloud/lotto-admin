package com.example.lotto.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>, WebMvcConfigurer {

    private final AdminTokenInterceptor adminTokenInterceptor;

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        // SPA 앱(React)의 클라이언트 라우팅 404 에러를 index.html로 포워드
        factory.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/index.html"));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 쓰기 API 관리자 토큰 검사 (경로는 context-path /lotto 제외 기준)
        registry.addInterceptor(adminTokenInterceptor).addPathPatterns("/api/v1/**");
    }
}
