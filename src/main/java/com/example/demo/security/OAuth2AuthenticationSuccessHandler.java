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

        // ★★★ 修正箇所: ResponseCookieビルダーを使い、SameSite属性を明示的に設定 ★★★
        ResponseCookie cookie = ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(true)       // HTTPSでのみ送信
                .path("/")
                .maxAge(Duration.ofHours(1))
                .sameSite("None")   // クロスサイトリクエストでもCookieを送信
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().write("""
               <h1>Login Successful!</h1>
               <p>You can now close this page.</p>
               <p>The accessToken cookie has been set in your browser.</p>
               """);
    }
}