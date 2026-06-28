package com.example.oauthjwt.config;

import com.example.oauthjwt.jwt.JWTFilter;
import com.example.oauthjwt.oauth2.CustomSuccessHandler;
import com.example.oauthjwt.service.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collections;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomSuccessHandler customSuccessHandler;
    private final JWTFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);

        // oauth2 설정
        http.oauth2Login((oauth2) -> oauth2
                .userInfoEndpoint((userInfoEndpointConfig) -> userInfoEndpointConfig
                        .userService(customOAuth2UserService)
                )
                .successHandler(customSuccessHandler)
        );

        // JWTFilter 필터 등록
        http.addFilterAfter(jwtFilter, OAuth2LoginAuthenticationFilter.class);

        // 인가 설정
        http.authorizeHttpRequests((auth) -> auth
                .requestMatchers("/").permitAll()
                .anyRequest().authenticated()
        );

        // 세션 설정: Stateless
        http.sessionManagement((session) -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // CORS 설정
        http.cors((corsCustomizer) -> corsCustomizer
                .configurationSource(new CorsConfigurationSource() {
                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {

                        CorsConfiguration configuration = new CorsConfiguration();

                        configuration.setAllowedOrigins(Collections.singletonList("http://localhost:3000"));
                        configuration.setAllowedMethods(Collections.singletonList("*"));
                        configuration.setAllowCredentials(true);
                        configuration.setAllowedHeaders(Collections.singletonList("*"));
                        configuration.setMaxAge(3600L);

                        configuration.setExposedHeaders(Collections.singletonList("Authorization"));

                        return configuration;
                    }
                })
        );

        return http.build();
    }
}
