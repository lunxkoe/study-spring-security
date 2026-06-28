package com.example.oauthjwt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OAuthJwtApplication {

    public static void main(String[] args) {
        SpringApplication.run(OAuthJwtApplication.class, args);
    }

}

// 동작 원리와 프로트/백 책임 분배
/*
> OAuth2 Code Grant 방식의 동작 순서
1. 로그인 페이지
2. 성공 후 코드 발급(redirect_uri)
3. 코드를 통해 Access 토큰 요청
4. Access 토큰 발급 완료
5. Access 토큰을 통해 유저 정보 요청
6. 유저 정보 획득 완료

> JWT 방식에서 OAuth2 클라이언트 구성시 고민점
- 로그인이 성공하면 JWT 발급 문제
    - 로그인이 성공하면 JWT를 발급해야하는 문제
        - 프론트단에서 로그인 경로에 대한 하이퍼링크를 실행하면 소셜 로그인창이 등장하고 로그인 로직을 수행
        - 로그인이 성공되면 JWT가 발급되는데, 하이퍼링크로 실행했기 때문에 JWT를 받을 로직이 없음
        - API Client(axios, fetch)로 요청을 보내면 백엔드측으로 요청이 전송되지만 외부 서비스 로그인 페이지를 확인할 수 없음

    - 웹/하이브리드/네이티브앱별 특징
        - 웹에서 편하게 사용할 수 있는 웹페이지가 앱에서는 웹뷰로 보이기 때문에 UX적으로 안좋은 경험을 가질 수 있음
        - 앱 환경에서 쿠키 소멸 현상

- 위와 같은 문제로 OAuth2 Code Grant 방식 동작에 대한 redirect_uri, Access 토큰 발급 문제를 어느 단에서 처리해야하는지에 대한 구현이 많고, 넷상에서 잘못된 구현 방법도 많이 있음

> 잘못된 구현 방법과 잘된 구현 방법
- 모든 책임을 프론트가 활용(잘못된 방식)
    - 프론트 단에서 유저 정보까지 발급받고 백엔드에 유저 정보를 전송 후 백엔드에서 JWT를 발급
    - 프론트에서 보낸 유저 정보의 진위 여부를 따지기 위해 추가적인 보안 로직이 필요함

- 책임을 프론트와 백엔드가 나누어 가짐(잘못된 방식)
    - 프론트 단에서 (로그인 -> 코드 발급) 후 코드를 백엔드로 전송
    - 백엔드 단에서 (코드 -> 토큰 발급 -> 유저 정보 획득 -> JWT 발급)

    - 프론트 단에서 Access 토큰까지 발급 후 백엔드로 전송
    - 해당 Access 토큰으로 JWT를 발급

    - 잘못된 이유
        - 코드/Access 토큰을 전송하는 방법을 지양함!!

- 모든 책임을 백엔드가 맡음(올바른 방법 / 이걸 할 것임)
    - 소셜 로그인 버튼
    - 하이퍼링크로 백엔드 API GET 요청
    - 로그인 요청 로직
    - 인증서버에서 로그인 페이지에서 로그인
    - 이후 세션 방식과 동일

    - 문제: 하이퍼링크로 요청했기때문에 JWT 획득이 까다로움
        - 하이퍼링크를 누르는 순간, 브라우저 주소창이 백엔드 주소로 바뀌면서 프론트엔드 앱 자체가 종료됨
        - 응답을 받을 코드가 없음, 백엔드에서 로그인 성공 후 다시 리다이렉션을 해주어야함
        - 토큰 전달 방법의 한계
            - 주소창에 넣어서 보내주기(X)
            - 쿠키에 굽기
*/

// 동작 원리
/*
> 전체적인 동작 원리
- 소셜 로그인 시도
    - /oauth2/authorization/서비스명
- 인증 서버에서 해당 서비스 로그인 페이지 응답
    - 성공하면 리다이렉트: /login/oauth2/code/서비스
    - 이때 code를 획득
- OAuth2LoginAuthenticationFilter에서 이 리다이렉트 요청을 가로챔
    - OAuth2LoginAuthenticationProvider에서 Code를 가지고 Access 토큰 발급
    - 이 Access 토큰을 가지고 리소스 서버에서 유저 정보를 획득
        - OAuth2Service에서 그 정보를 획득 / OAuth2User여기에 담음

- 로그인을 성공하면
    - LoginSuccessHandler가 동작
    - 여기서 JWT를 발급

- 아무 요청 "/**"
    - JWT 검증 후 존재 하면 세션 생성(JWTFilter에서 확인)

> JWTFilter: 우리가 커스텀해서 등록해야함
    - 모든 주소에서 동작

> OAuth2AuthorizationRequestRedirectFilter
    - /oauth2/authorization/서비스명
    - /oauth2/authorization/naver
    - /oauth2/authorization/google

> OAuth2LoginAuthenticationFilter: 외부 인증 서버에 설정할 Redirect_URI
    - /login/oauth2/code/서비스명

> 구현할 부분
- OAuth2UserDetailsService
- OAuth2UserDetails
- LoginSuccessHandler

- JWT Filter
- JWTUtil: JWT 발급 및 검증하는 클래스

> 요약
- OAuth2UserDetailsService: 소셜 서버에서 받아온 유저 정보를 우리 서비스에 맞게 처리하는 서비스 로직.
- OAuth2UserDetails: 가져온 유저 정보를 담아둘 커스텀 객체 (DTO 역할).
- LoginSuccessHandler: 로그인이 모두 성공했을 때 "그럼 이제 JWT를 만들어서 줘라"라고 지시하는 핸들러.
- JWTUtil: 실제로 JWT 문자열을 찍어내고(생성), 나중에 뜯어보는(검증) 도구 클래스.
-JWTFilter: 문지기처럼 모든 API 요청 앞에 서서 JWTUtil을 써서 토큰이 진짜인지 검사하는 필터.
*/

/*
- OAuth2UserDetailsService: 소셜 서버에서 받아온 유저 정보를 우리 서비스에 맞게 처리하는 서비스 로직.
- OAuth2UserDetails: 가져온 유저 정보를 담아둘 커스텀 객체 (DTO 역할).
        - LoginSuccessHandler: 로그인이 모두 성공했을 때 "그럼 이제 JWT를 만들어서 줘라"라고 지시하는 핸들러.
- JWTUtil: 실제로 JWT 문자열을 찍어내고(생성), 나중에 뜯어보는(검증) 도구 클래스.
        -JWTFilter: 문지기처럼 모든 API 요청 앞에 서서 JWTUtil을 써서 토큰이 진짜인지 검사하는 필터.
*/

// OAuth2UserService 응답 받기