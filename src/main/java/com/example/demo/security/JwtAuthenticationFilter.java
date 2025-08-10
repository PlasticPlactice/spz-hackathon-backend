package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && tokenProvider.validateToken(token)) {
            SecurityContextHolder.getContext().setAuthentication(tokenProvider.getAuthentication(token));
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // 1. Authorizationヘッダー（Bearer）から取得
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.trim().toLowerCase().startsWith("bearer ")) {
            // 大文字・小文字・前後スペースを許容し、正しくトークンを抽出
            return bearerToken.trim().substring(7).trim();
        }
        // 2. Cookieから取得（従来通り）
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> "accessToken".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
