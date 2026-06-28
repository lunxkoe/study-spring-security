package com.example.oauthsession.config;

import com.example.oauthsession.oauth2.CustomClientRegistrationRepo;
import com.example.oauthsession.oauth2.CustomOAuth2AuthorizedClientService;
import com.example.oauthsession.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomClientRegistrationRepo customClientRegistrationRepo;
//    private final CustomOAuth2AuthorizedClientService customOAuth2AuthorizedClientService;
//    private final JdbcTemplate jdbcTemplate;
    private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf((auth) -> auth.disable());
        http.formLogin((login) -> login.disable());
        http.httpBasic((basic) -> basic.disable());

        // OAuth2 설정
        // - oauth2Login이 모든 필터와 세팅을 자동으로 진행해줌
        // - oauth2Client는 직접 커스텀 해야함
        // - Customizer.withDefaults(): 일단 기본 제공 설정
        // - 이제 우리가 만든 UserService를 등록해주어야함
        http.oauth2Login((oauth2) -> oauth2
                .userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig
                        .userService(customOAuth2UserService)) // 데이터를 받을 수 있는 UserService의 엔드 포인트
                .loginPage("/login") // 우리의 컨트롤러 경로를 등록해주면 GET /login으로 오는 요청을 여기로 보냄
                .clientRegistrationRepository(customClientRegistrationRepo.clientRegistrationRepository())
                .authorizedClientService(oAuth2AuthorizedClientService)
        );

        // 인가 설정
        http.authorizeHttpRequests((auth) -> auth
                .requestMatchers("/", "/oauth2/**", "/login/**").permitAll()
                .anyRequest().authenticated()
        );

        return http.build();
    }
}
