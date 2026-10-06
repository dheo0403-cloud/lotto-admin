package com.example.lotto.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 쓰기(POST) API는 X-Admin-Token 헤더가 관리자 토큰과 일치할 때만 허용한다.
 * 토큰이 설정되지 않았으면 쓰기 API를 전부 차단한다(fail-closed). 조회(GET)는 검사하지 않는다.
 */
@Component
public class AdminTokenInterceptor implements HandlerInterceptor {

    private final byte[] token;

    public AdminTokenInterceptor(@Value("${app.admin-token:}") String token) {
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;

        String given = request.getHeader("X-Admin-Token");
        // 상수 시간 비교로 타이밍 공격 방지
        if (token.length == 0 || given == null
                || !MessageDigest.isEqual(token, given.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        return true;
    }
}
