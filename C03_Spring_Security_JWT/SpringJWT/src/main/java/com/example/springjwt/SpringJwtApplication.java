package com.example.springjwt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringJwtApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringJwtApplication.class, args);
    }

}

// JWT
/*
> JWT 구조
- Header:
    - JWT임을 명시
    - 사용된 암호화 알고리즘
- Payload:
    - 정보
- Signature:
    - 암호화 알고리즘(BASE64(Header) + BASE64(Payload) + 암호화키)

- 주의사항: 복호화할 수 있음
    - 유저를 검증할 수 있는 것만 담아야함!! (외부에서 열람해도 되는 정보만 넣어야함)
- 그럼 왜 사용하는가?
    - 내 서버에서 발급한 것임만 증명하면 됨(발급처만 검증)

> JWT 암호화 방식
- 암호화 종류
    - 양방향
        - 대칭키(사용)
        - 비대칭키
    - 단방향

> 암호화 키 저장
- 암호화 키는 하드코딩 방식으로
- 구현 내부에서 탑재하는 것을 지양, 변수 설정 파일에 저장해야함!! (공부용이므로 application.yaml에 저장)

> JWTUtil
- 토큰 Payload에 저장될 정보
    - username
    - role
    - 생성일
    - 만료일

- JWTUtil 구현 메소드
    - JWTUtil 생성자
    - username 확인 메소드
    - role 확인 메소드
    - 만료일 확인 메소드
*/

// 토큰 검증 필터
/*
> 현재 상황
- 앞으로 로그인을 하고, 이후 특정 경로에 접근할 때, 발급된 토큰을 헤더에 넣어서 같이 전송
- 근데 지금은 인증이 안되는 상황
    - 토큰을 받고 확인하는 걸 구현하지 않았기 때문
*/

// 세션 정보
/*
> 무상태 세션
- 기존에 쿠키-세션 방식에서는 세션 자체를 애플리케이션 서버 내부에 가지고 있지만
- JWT 방식에서는 세션을 무상태로 만듦

> 그러면 어떻게 사용자의 정보를 가져올 수 있을까?
- 로그인을 하고 헤더에 JWT 토큰 값을 넣어서 접근을 시도하면, 이제 JWTFilter가 동작함
- 토큰 값이 존재하면, 토큰을 까서 SecurityContextHolder에 임시 세션을 만들어서 값을 할당하고 이제 그걸 꺼내서 사용할 수 있음

> 꺼내는 방법
- 컨트롤러에서만 가능한 방법
    - @AuthenticationPrincipal CustomUserDetails userDetails을 함수 인자로 사용하면 꺼내올 수 있음

- 그외 SecurityContextHolder
```java
@GetMapping("/")
    public String mainP() {
        // username 가져오기 (아이디)
        String name = SecurityContextHolder.getContext().getAuthentication().getName();

        // authentication에서 가져오는 방법
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // username 가져오기
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        // 권한 가져오는 방법
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

        return "main controller" + name + " " + username + " " + userDetails.getPassword() + " " + role;
    }
```
    - 이런식으로 사용할 수 있음
*/

// CORS 설정
/*
> CORS란
- 프론트엔드랑 백엔드 서버를 따로 띄우면, CORS 문제 때문에 데이터가 제대로 날아오지 않거나,
- 그런 데이터를 보여줄 수 없는 문제가 발생할 수 있음

> 발생 원리
- 프론트엔드 서버 (3000)
- 백엔드 서버 (8080)
- 두 개의 서버 포트가 다르기 때문에 웹브라우저에서 "교차 출처 리소스 공유를 금지"
- 백엔드 단에서 CORS 설정을 해주어야만 함
    - 토큰이 리턴되지 않는 문제가 발생할 수 있음

> CORS 설정
- SecurityConfig
```java
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
```
- config>CorsMvcConfig
```java
@Configuration
public class CorsMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {
        corsRegistry.addMapping("/**")
                .allowedOrigins("http://localhost:3000");
        // - CORS 허용을 해줄 Method(모든 경로에 대해서 3000번에서 오는 요청을 허용
    }
}
```
*/
