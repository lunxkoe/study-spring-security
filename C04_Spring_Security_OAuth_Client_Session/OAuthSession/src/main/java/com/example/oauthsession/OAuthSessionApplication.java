package com.example.oauthsession;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OAuthSessionApplication {

    public static void main(String[] args) {
        SpringApplication.run(OAuthSessionApplication.class, args);
    }

}

// OAuth2 소셜 로그인을 위한 변수 설정
/*
> application.yaml
```
#registration - 네이버 서버에서 우리 서버를 식별하기 위한 것(필수적)
spring.security.oauth2.client.registration.서비스명.client-name=서비스명
spring.security.oauth2.client.registration.서비스명.client-id=서비스에서 발급 받은 아이디
spring.security.oauth2.client.registration.서비스명.client-secret=서비스에서 발급 받은 비밀번호
spring.security.oauth2.client.registration.서비스명.redirect-uri=서비스에 등록한 우리쪽 로그인 성공 URI
spring.security.oauth2.client.registration.서비스명.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.서비스명.scope=리소스 서버에서 가져올 데이터 범위

#provider - 네이버 서버의 주소를 넣을 것이기 때문에 유명한 서비스의 경우 OAuth2 Client가 이미 가지고 있음(구글, 옥타, 페이스북, 깃허브 O / 네이버 X)
spring.security.oauth2.client.provider.서비스명.authorization-uri=서비스 로그인 창 주소
spring.security.oauth2.client.provider.서비스명.token-uri=토큰 발급 서버 주소
spring.security.oauth2.client.provider.서비스명.user-info-uri=사용자 정보 획득 주소
spring.security.oauth2.client.provider.서비스명.user-name-attribute=응답 데이터 변수
```

```
#registration
spring.security.oauth2.client.registration.naver.client-name=naver
spring.security.oauth2.client.registration.naver.client-id=발급아이디
spring.security.oauth2.client.registration.naver.client-secret=발급비밀번호
spring.security.oauth2.client.registration.naver.redirect-uri=http://localhost:8080/login/oauth2/code/naver
spring.security.oauth2.client.registration.naver.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.naver.scope=name,email

#provider
spring.security.oauth2.client.provider.naver.authorization-uri=https://nid.naver.com/oauth2.0/authorize
spring.security.oauth2.client.provider.naver.token-uri=https://nid.naver.com/oauth2.0/token
spring.security.oauth2.client.provider.naver.user-info-uri=https://openapi.naver.com/v1/nid/me
spring.security.oauth2.client.provider.naver.user-name-attribute=response # User 정보가 response라는 키에 담겨옴
```
*/

// OAuth2UserService 구현
/*
> 네이버와 구글이 보내는 인증 규격이 다름
- OAuth2Response Interface
    - NaverResponse
    - GoogleResponse

> OAuth2Response
- 네이버 데이터
```json
{
		resultcode=00, message=success, response={id=123123123, name=개발자유미}
}
```

- 구글 데이터
```json
{
		resultcode=00, message=success, id=123123123, name=개발자유미
}
```
*/

// 응답 데이터로 로그인 완료
/*

*/

// 유저 정보 DB 저장
/*
> 동작 흐름
- OAuth2UserService에서 Provider ID를 가지고 이미 존재하는 회원인지 DB에 조회
    - 없으면 신규 저장
    - 있으면 업데이트
*/

// 커스텀 로그인 페이지
/*
> 기본 로그인 페이지
- GET: /login으로 OAuth2Client가 제공해주는 페이지로 이동함

> 커스텀 로그인 페이지 설정
- login.mustache 생성
```html
<hr>
<a href="/oauth2/authorization/naver">naver login</a><br>
<a href="/oauth2/authorization/google">google login</a>
```

- LoginController 작성
- SecurityConfig에 경로를 설정
    - .loginPage("/login")
*/

// Client Registration
/*
> OAuth2 서비스 변수 등록 방법
- 기존에는 application.yaml 파일에 필요한 변수를 등록하는 방법
- 클래스를 통해 직접 진행하는 방법
    - ClientRegistration: 서비스 별 OAuth2 클라이언트의 등록 정보를 가지는 클래스
    - ClientRegistrationRepository: ClientRegistration의 저장소로 서비스별 ClientRegistration들을 가짐
- 이후 Security Config에 설정 등록
*/

// OAuth2AuthorizationRequestRedirectFilter
/*
> 로그인 페이지에서 /oauth2/authorization/서비스
- 이 경로로 GET 요청을 보냄
- OAuth2AuthorizationRequestRedirectFilter 이 요청을 받으면 해당하는 인증 서버로 리다이렉션
- 거기서 로그인 창을 던져줌

> 내부 동작
- 서비스의 정보는 ClientRegistrationRepository에서 가져옴(또는 application.yaml)
- OncePerRequestFilter를 상속받아서 만들어져잇음
    - 하나의 Request에 대해서 딱 한 번만 실행되도록 보장
*/

// OAuth2LoginAuthenticationFilter
/*
> 로그인 성공: redirect_uri 필터: /login/oauth2/code/서비스명
- 인증 서버에서 로그인을 성공하며 code를 보내줌
- 이 code를 받는 곳임
- 이 코드를 통해서 OAuth2LoginAuthenticationProvider에서 Access 토큰을 발급받고(내부적으로 알아서), 리소스 서버에서
- 유저 정보를 획득함

> OAuth2LoginAuthenticationFilter
- /login/oauth2/code/서비스명으로 들어오는 요청을 가로챔

> OAuth2LoginAuthenticationProvider
- 여기서 코드를 받고 Access 토큰을 내부적으로 알아서 생성해서 리소스 서버로부터 유저 정보를 획득함
*/

// OAuth2AuthorizedClientService (매우 중요함)
/*
> OAuth2AuthorizedClientService
- 소셜 로그인을 진행하는 사용자에 대해 우리의 서버는 인증 서버에서 발급 받은 Access 토큰과 같은 정보를 담을 저장소가 필요함
- 기본적으로 인메모리 방식으로 관리되는데, 소셜 로그인 사용자 수가 증가하고, 서버의 스케일 아웃 문제로 인해 인메모리 방식은 실무에서 사용하지 않음
- 따라서 DB에 해당 정보를 저장하기 위한 OAuth2AuthorizedClientService를 직접 구현해야함

> 의존성 추가
- JDBC 방식을 지원하기 때문에 JDBC 모듈이 필요하고 JPA를 사용하고 싶은 경우 전부 커스텀해서 만들어야함

> CustomOAuth2AuthorizedClientService JDBC로 생성
- 이후 Security Config에 등록

> DB 매핑될 테이블 생성
```sql
CREATE TABLE oauth2_authorized_client (
  client_registration_id varchar(100) NOT NULL,
  principal_name varchar(200) NOT NULL,
  access_token_type varchar(100) NOT NULL,
  access_token_value blob NOT NULL,
  access_token_issued_at timestamp NOT NULL,
  access_token_expires_at timestamp NOT NULL,
  access_token_scopes varchar(1000) DEFAULT NULL,
  refresh_token_value blob DEFAULT NULL,
  refresh_token_issued_at timestamp DEFAULT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (client_registration_id, principal_name)
);
```

> 의문사항
- (client_registration_id, principal_name)가 겹치는 경우가 존재하면 테이블에 값이 오버라이딩되는 현상이 발생
- 문제가 없을까?
- 예시
```
user: username / client_registration_id / principal_name
user1: aaaa / naver / 개발자유미
user2: bbbb / naver / 개발자유미
```
- 두 값이 겹치면 새로운 row가 생성되는 것이 아니라, 기존 데이터 위에 덮어쓰워짐
- 답변:
    - OAuth2AuthorizedClientService를 알아서 구현해서 사용해야함
*/

// 실행 흐름
/*
> 대장 필터: OAuth2LoginAuthenticationFilter
1. 대장 필터: 구글에서 엑세스 토큰 획득
2. CustomOAuth2UserService야. 이 토큰으로 유저 정보를 가져와줘.
3. UserService: (DB 회원 가입 후). CutstomOAuth2User 객체 반환
4. 대장 필터: 로그인을 끝내기 전에 토큰부터 DB에 저장하자
    - CustomOAuth2AuthorizedClientService야. 내가 지금 들고 있는 액세스 토큰이랑 CustomOAuth2User 객체 둘 다 줄테니 DB에 넣어줘
5. AuthorizedClientService: 받은 객체에서 .getName()을 꺼내 PK로 사용 (principal_name) DB에 insert/update 실행
6. 대장 필터: DB 저장 끝, CustomOAuth2User를 세션에 저장하고 로그인 종료
*/