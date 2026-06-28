package study.security6;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestSecurity6Application {

    public static void main(String[] args) {
        SpringApplication.run(TestSecurity6Application.class, args);
    }

}

// 로그인 정보
// - 사용자가 로그인을 하면 사용자 정보는 SecurityContextHolder에 의해서 서버 세션에 관리됟
// - 이때 세션에 관해 세션의 소멸 시간, 아이디당 세션 생성 개수를 설정할 수 있음

// 세션 소멸 시간 설정
// - 로그인 이후 세션이 유지되고 소멸하는 시간을 설정할 수 있음
// - 세션 소멸 시점은 서버에 마지막 특정 요청을 수행한 뒤 설정한 시간 만큼 유지된 (기본 시간 1800초 (30분))

// 다중 로그인 설정
// - 동일한 아이디로 다중 로그인을 진행할 경우에 대한 설정 방법은 세션 통제를 통해 진행함

// 세션 고정 보호
// - 세션 중간 가로채기라고 잏단 이해

// CSRF
/*
> CSRF란?
- Cross Site Request Forgery는 요청을 위조하여 사용자가 원하지 않아도, 서버측으로 특정 요청을 강제로 보내는 방식

> 개발 환경에서 csrf disable()

> enable 설정 시, 스프링 시큐리티는 CsrfFilter를 통해 POST, PUT, DELETE 요청에 대해서 토큰 검증
*/

// HttpBasic 인증
/*
> 로그인 방식
- loginForm
- httpBasic
    - 아이디와 비밀버호를 Base64 방식으로 인코딩한 후 HTTP 인증 헤더에 부착하여 서버측으로 요청을 보내는 방식
*/

// Role Hirarchy
/*
> 계층형 권한
- A < B < C
- 높으면 들어가도 됨
*/