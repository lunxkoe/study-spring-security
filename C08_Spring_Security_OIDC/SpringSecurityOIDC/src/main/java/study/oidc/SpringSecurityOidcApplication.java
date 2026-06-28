package study.oidc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringSecurityOidcApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringSecurityOidcApplication.class, args);
    }

}

// OAuth2와 OIDC
/*
> OIDC
- OpenID Connect

> 기존 OAuth2 Code 방식 인증 흐름과 OIDC 비교
- 기존 OAuth2 Code 방식
    - 로그인 시도: /oauth2/authorization/서비스명
        - OAuth2AuthorizationRequestRedirectFilter가 이 요청을 가로챔
        - 서비스명 쪽의 인증 서버로 요청을 보냄
        - 해당 인증 서버에서 로그인 창을 보내줌
        - 해당 로그인 창에서 로그인
    - 로그인 성공: /login/oauth2/code/서비스명
        - 인증 서버에서 Code를 우리 서버로 Code를 보내줌
        - OAuth2LoginAuthenticationFilter가 그 Code를 받음
            - OAuth2AuthenticationProvider에서 Code를 가지고 인증 서버로부터 Access 토큰을 발급받음
            - 이 Access 토큰으로 유저 정보를 서비스의 리소스 서버로부터 획득함
    - OAuth2UserDetails / OAuth2UserDetailsService에서 그 요청을 받아서 자체적으로 처리하면 됨

- OIDC 방식
    - 로그인 성공까지는 동일함
        - OidcAuthorizationCodeAuthenticationProvider가 Code를 기반으로 Access 토큰과 id 토큰을 발급받음
        - id 토큰 내부
            - 유저 이름
            - 유저 이메일
            - UUID
            - 식별할 수 있는 메타 정보를 함께 넘겨줌
            - **즉, 따로 리소스 서버에 접근하지 않아도 됨**
    - OidcUserService에서 동일하게 진행하면 됨

> 진실
- OAuth2는 사실 권한을 위임하는 프로토콜이라 정확한 유저 정보는 그 과정에서 받은 토큰으로 리소스 서버에 접근해햐 함
- 반면 OIDC는 유저 정보를 포함하는 id 토큰으로 그 과정을 수행할 수 있음
- 장점
    - **표준화 됨**: id 토큰 payload가 표준화 되어있어 서비스 제공자가 OIDC만 지원한다면 어떤 데이터를 받는지 예측 가능
        - OAuth2에서는 제공자마다 어떤 데이터를 주는지 공식 문서를 읽고 맞춰줘야함
    - SSO 구현 쉬움 id 토큰만 내려주면 됨
*/

// OIDC 네이버, 구글 OIDC 변수 등록
/*
> 주의점
- 서비스 제공 업체의 OIDC 지원 여부
- OIDC 지원 api 주소(OAuth2와 조금 다름)
- scope 설정

> Scope 설정(scope=openid)
- 단 하나 scope 설정에 무조건 openid가 포함되어야 함
- 이 값을 명시하면 OAuth2 클라이언트 의존성이 OIDC 로그인으로 판단함
- 이유: Provider 선택
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          naver:
            client-name: naver
            client-id: ${NAVER_CLIENT_ID}
            client-secret: ${NAVER_CLIENT_SECRET}
            redirect-uri: http://localhost:8080/login/oauth2/code/naver
            authorization-grant-type: authorization_code
            scope: openid # OIDC 사용

          google:
            client-name: google
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            redirect-uri: http://localhost:8080/login/oauth2/code/google
            authorization-grant-type: authorization_code
            scope: openid # OIDC 사용
        provider:
          naver:
            issuer-uri: https://nid.naver.com
            authorization-uri: https://nid.naver.com/oauth2/authorize
            jwk-set-uri: https://nid.naver.com/oauth2/jwks
            token-uri: https://nid.naver.com/oauth2/token
```
*/

// OidcUserService 데이터 커스텀
/*
> default
- 116855853446826670456 : OIDC_USER
    - UUID(username) : ROLE(role)

> 커스텀
- OIDC를 파싱하는 OidcUserService를 커스텀 해야함
```java
@Service
public class CustomOidcUserService extends OidcUserService {

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

        OidcUser oidcUser = super.loadUser(userRequest);

        System.out.println(oidcUser);
        System.out.println(oidcUser.getIdToken());
        System.out.println(oidcUser.getUserInfo());
        System.out.println(oidcUser.getClaims());

        // ROLE 만들어 넣기
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }
}
```

```java
public class CustomOidcUser extends DefaultOidcUser {

    private final Long memberId;
    private final String nickname;

    public CustomOidcUser(
            Collection<? extends GrantedAuthority> authorities,
            OidcIdToken idToken,
            OidcUserInfo userInfo,
            Long memberId,
            String nickname
    ) {
        super(authorities, idToken, userInfo);
        this.memberId = memberId;
        this.nickname = nickname;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getNickname() {
        return nickname;
    }
}
```

```java
@Service
public class CustomOidcUserService extends OidcUserService {

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

        OidcUser oidcUser = super.loadUser(userRequest);

        System.out.println(oidcUser);
        System.out.println(oidcUser.getIdToken());
        System.out.println(oidcUser.getUserInfo());
        System.out.println(oidcUser.getClaims());

        // ROLE 만들어 넣기
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        String email = oidcUser.getEmail();
        Long customMemberId = 1004L;
        String customNickname = email.split("@")[0] + "천사";

//        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        return new CustomOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), customMemberId, customNickname);
    }
}
```

```java
@RestController
public class MainController {

    @GetMapping("/")
    public String index(@AuthenticationPrincipal CustomOidcUser oidcUser) {
//        String username = SecurityContextHolder.getContext().getAuthentication().getName();
//        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().toString();

//        String username = oidcUser.getEmail();
//        String role = oidcUser.getAuthorities().iterator().next().toString();

        String username = oidcUser.getNickname();
        String role = oidcUser.getAuthorities().iterator().next().toString();

        return username + " : " + role;
    }
}
```
*/