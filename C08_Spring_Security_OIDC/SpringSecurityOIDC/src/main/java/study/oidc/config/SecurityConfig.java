package study.oidc.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import study.oidc.oidc.CustomOidcUserService;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOidcUserService customOidcUserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);

        // CustomOidcUserService 등록
        http.oauth2Login((oauth2) -> oauth2
                .userInfoEndpoint((userInfo) -> userInfo
                        .oidcUserService(customOidcUserService)
                )
        );

        http.authorizeHttpRequests((auth) -> auth
                .requestMatchers("/**").permitAll()
                .anyRequest().authenticated()
        );

        return http.build();
    }
}
