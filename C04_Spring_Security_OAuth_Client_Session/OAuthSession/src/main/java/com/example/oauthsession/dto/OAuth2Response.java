package com.example.oauthsession.dto;

public interface OAuth2Response {
    // 네이버 / 구글
    String getProvider();

    // 각각의 유저에 대해서 번호를 부여해줌
    String getProviderId();

    // 사용자의 이메일
    String getEmail();

    // 사용자의 이름
    String getName();
}
