package com.example.demo.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;

    public OAuth2AuthenticationSuccessHandler(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String token = tokenProvider.createToken(authentication);

        // ★★★ 修正箇所: 正しいビルダーのクラス名に変更 ★★★
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofHours(1));

        boolean isSecure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        if (isSecure) {
            cookieBuilder.secure(true).sameSite("None");
        }

        ResponseCookie cookie = cookieBuilder.build();
        response.addHeader("Set-Cookie", cookie.toString());

        String targetUrl = "http://localhost:3000/dashboard";
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}