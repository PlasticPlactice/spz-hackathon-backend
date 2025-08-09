package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long validityInMilliseconds = 3600000; // 1時間

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(Authentication authentication) {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        Long githubId = Long.valueOf(oauth2User.getAttributes().get("id").toString());
        String username = oauth2User.getAttributes().get("login").toString();

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(githubId.toString())
                .claim("username", username)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        String githubId = claims.getSubject();

        // ここではシンプルな権限を付与
        return new UsernamePasswordAuthenticationToken(githubId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // トークンが無効な場合はfalseを返す
            return false;
        }
    }

}