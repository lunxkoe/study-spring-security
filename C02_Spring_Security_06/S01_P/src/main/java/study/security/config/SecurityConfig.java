package study.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 비밀번호 해시화 도구
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // CSRF 설정(개발 - 비활성 / 배포 - 반드시 활성)
        http.csrf((auth) -> auth.disable());

        // 접속 가능 URL
        http.authorizeHttpRequests((auth) -> auth
                .requestMatchers("/", "/join").permitAll()
                .requestMatchers("/admin").hasRole("ADMIN")
                .requestMatchers("/my/**").hasAnyRole("ADMIN", "USER")
                .anyRequest().authenticated()
        );

        // formLogin 방식 로그인
        // - username으로 폼에서 들어오는 것을 기본으로 받음
        // - spring security에서 username에 해당하는 것이 흔히 말하는 ID
        // - UserDetails에서 내가 반환하는 값이므로 내가 email, id, username(사용자명) 뭘 넣냐에 따라 바뀜
        // - 단, 흔히 말하는 ID라는게 spring security에서 통상적으로 username으로 부름
        http.formLogin((auth) -> auth
                .loginPage("/login")
                // - 기본 제공 로그인 페이지를 없애고 사용자가 직접 작성한 페이지를 보여줌
                .loginProcessingUrl("/login-proc")
                .permitAll()
        );

        // httpBasic 방식 로그인
//        http.httpBasic(Customizer.withDefaults());

        // 로그아웃
        // - CSRF 활성화 시 POST 요청으로만 로그아웃을 진행해야 함 / 꺼져있으면 /logout으로도 동작
        // - LogoutController를 따로 만들어서 GET으로 진행할 수 있음
        http.logout((auth) -> auth
                .logoutUrl("/logout")
                .logoutSuccessUrl("/") // 로그아웃 성공 시 리다이렉트 할 URL
        );

        // 다중 로그인 설정
        http.sessionManagement((auth) -> auth
                        .maximumSessions(1) // 하나의 아이디에 대한 다중 로그인 허용 개수
                        .maxSessionsPreventsLogin(true)
                // - true: 초과 시 새로운 로그인 차단
                // - false: 초과시 기존 세션 하나 삭제
        );

        // 세션 고정 보호
        http.sessionManagement((auth) -> auth
                        .sessionFixation().changeSessionId()
                // - .none(): 로그인 시 세션 정보 변경 안함
                // - .newSession(): 로그인 시 세션 새로 생성함
                // - .changeSessionId(): 로그인 시 동일한 세션에 대한 id 변경 (주로 사용)
        );

        return http.build();
    }

    // 계층형 권한 설정
    @Bean
    public RoleHierarchy roleHierarchy() {

//        return RoleHierarchyImpl.fromHierarchy("""
//                ROLE_C > ROL_B
//                ROLE_B > ROLE_A
//                """);

        // ROLE이라는 접두사를 자동으로 붙여줌
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("USER")
                .build();
    }
}
