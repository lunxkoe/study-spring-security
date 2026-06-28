package com.example.springjwt.config;

import com.example.springjwt.jwt.JWTUtil;
import com.example.springjwt.jwt.JwtFilter;
import com.example.springjwt.jwt.LoginFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collections;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // 이거는 스프링 부트가 생성해주는듯?
    private final AuthenticationConfiguration authenticationConfiguration;
    private final JWTUtil jwtUtil;
    private final JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // csrf disable
        // - 세션 방식에서는 세션이 항상 고정되기 때문에 csrf 공격을 받을 수 있음
        // - JWT에서는 stateless로 동작하기 때문에 csrf 공격을 방어해주지 않아도 됨
        http.csrf((auth) -> auth.disable());

        // formLogin disable
        http.formLogin((auth) -> auth.disable());

        // http basic disable
        http.httpBasic((auth) -> auth.disable());


        // URL authentication config (인가 설정)
        http.authorizeHttpRequests((auth) -> auth
                .requestMatchers("/", "/login", "/join").permitAll()
                .requestMatchers("/admin").hasRole("ADMIN")
                .anyRequest().authenticated()
        );

        // At: 특정 위치에 필터 등록
        http.addFilterAt(
                new LoginFilter(authenticationManager(authenticationConfiguration), jwtUtil),
                UsernamePasswordAuthenticationFilter.class
        );

        http.addFilterBefore(jwtFilter, LoginFilter.class);

        // CORS 설정
        http.cors((cors) -> cors.configurationSource(
                new CorsConfigurationSource() {
                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {

                        CorsConfiguration configuration = new CorsConfiguration();

                        configuration.setAllowedOrigins(Collections.singletonList("http://localhost:3000"));
                        // - 허용할 포트
                        configuration.setAllowedMethods(Collections.singletonList("*"));
                        // - 허용할 Method (GET/POST/PUT/DELETE 같은)
                        configuration.setAllowCredentials(true);
                        configuration.setAllowedHeaders(Collections.singletonList("*"));
                        // - 허용할 헤더
                        configuration.setMaxAge(3600L);
                        // - 허용을 물고 있을 시간

                        configuration.setExposedHeaders(Collections.singletonList("Authorization"));
                        // - 나중에 클라이언트한테 Authorization에 JWT를 넣어서 보내줄거기 때문에 허용

                        return configuration;
                    }
                }
        ));

        // 세션 설정
        // - **JWT에서는 세션을 stateless 상태로 관리함**
        http.sessionManagement((session) -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        return http.build();
    }
}
