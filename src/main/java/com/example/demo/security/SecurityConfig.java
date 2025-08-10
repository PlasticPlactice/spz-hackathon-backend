package com.example.demo.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Autowired
    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler, JwtAuthenticationFilter jwtAuthenticationFilter, CustomAuthenticationEntryPoint customAuthenticationEntryPoint) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                //CSRFを無効化し、セッション管理をステートレスにする
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/login-success", "/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )
                //OAuth2ログイン処理の前に、自作のJWTフィルターを実行する
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    // CORS設定の詳細を定義するBean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // フロントエンドのURLを許可
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "https://spz-hackathon-backend.onrender.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // Cookieをやり取りするために必要
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 全てのパスに対してこのCORS設定を適用
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
