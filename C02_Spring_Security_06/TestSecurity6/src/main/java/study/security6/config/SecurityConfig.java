package study.security6.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity // Spring Security에 의해서 관리됨
public class SecurityConfig {

    // BCrypt 암호화
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 여기에 설정 적용

        // 해당 경로로 오는 접근에 대한 필터링 진행
        // - 인가 동작 순서: 위에서 아래로 진행
        //      - 주의사항: 상단에서 모든 경로에 대해서 permitAll()을 하면 밑으로 들어오지 않음
        //      - 구체적인 것부터 위에서부터 아래로 진행
        http.authorizeHttpRequests((auth) -> auth
                .requestMatchers("/", "/login", "/loginProc", "/join", "/joinProc").permitAll() // 모두 접근 가능
                .requestMatchers("/admin").hasRole("ADMIN") // 권한이 있는 사용자만 접근 가능
                .requestMatchers("/my/**").hasAnyRole("ADMIN", "USER") // 권한이 있는 사용자만 접근 가능
                .anyRequest().authenticated() // 로그인한 사용자만 접근 가능
        );

        // config를 등록하면 리다이렉팅을 직접 해주어야함

        // login 페이지로 리다이렉팅 설정
        http.formLogin((auth) -> auth
                .loginPage("/login") // GET으로 호출됨
                .loginProcessingUrl("/loginProc") // 이 경로 로그인 데이터를 넘기면 Spring Security가 받아서 처리해줌
                .permitAll()
        );

        // httpBasic 방식
//        http.httpBasic(Customizer.withDefaults());

        // CSRF 설정 시 POST 요청으로 로그아웃을 진행해야만 함
        // - LogoutController를 따로 만들어서 GET으로 진행할 수 있음
        http.logout((auth) -> auth
                .logoutUrl("/logout") // POST 요청으로 로그아웃(CSRF 활성화 상태)
                .logoutSuccessUrl("/") // 로그인 성공 시 갈 곳
        );

        // 현재 개발 환경에서 이것을 켜두면 토큰을 보내기 않으면 로그인이 안됨
        // - 개발에서만 이것을 꺼줌
        // - 나중에 다시 Enable 시켜줘야 함
//        http.csrf((auth) -> auth.disable());

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

    // 계층형 권한 (6.3.x)
    @Bean
    public RoleHierarchy roleHierarchy() {

        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_C > ROL_B
                ROLE_B > ROLE_A
                """);

        // ROLE이라는 접두사를 자동으로 붙여줌
//        return RoleHierarchyImpl.withDefaultRolePrefix()
//                .role("C").implies("B")
//                .role("B").implies("A")
//                .build();
    }

}
