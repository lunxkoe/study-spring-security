package com.example.oauthsession.oauth2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.stereotype.Component;

@Component
public class SocialClientRegistration {

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    public ClientRegistration naverClientRegistration() {
        return ClientRegistration.withRegistrationId("naver")
                .clientId(naverClientId) // 하드코딩 대신 변수 주입
                .clientSecret(naverClientSecret) // 하드코딩 대신 변수 주입
                .redirectUri("http://localhost:8080/login/oauth2/code/naver")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .scope("name", "email")
                .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
                .tokenUri("https://nid.naver.com/oauth2.0/token")
                .userInfoUri("https://openapi.naver.com/v1/nid/me")
                .userNameAttributeName("response")
                .build();
    }

    // 여기서는 Provider도 직접 값을 넣어주어야함
    public ClientRegistration googleClientRegistration() {
        return ClientRegistration.withRegistrationId("google")
                .clientId(googleClientId) // 하드코딩 대신 변수 주입
                .clientSecret(googleClientSecret) // 하드코딩 대신 변수 주입
                .redirectUri("http://localhost:8080/login/oauth2/code/google")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .scope("profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .build();
    }
}
