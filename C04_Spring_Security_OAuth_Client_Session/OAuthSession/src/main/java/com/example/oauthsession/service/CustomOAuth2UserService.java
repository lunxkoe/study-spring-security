package com.example.oauthsession.service;

import com.example.oauthsession.dto.CustomOAuth2User;
import com.example.oauthsession.dto.GoogleResponse;
import com.example.oauthsession.dto.NaverResponse;
import com.example.oauthsession.dto.OAuth2Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    // DefaultOAuth2UserService: OAuth2UserService를 구현한 구현체이므로 상속받아도 상관없음

    private final OAuth2UserDbService oAuth2UserDbService;

    // 네이버나 구글의 사용자 정보 데이터를 내부 인자로 받아옴
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("oauth2User.getAttributes(): {}", oAuth2User.getAttributes());

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        // - 네이버인지 구글인지 어떤 인증 Provider인지 확인

        // 나누는 이유
        // - 보내주는 인증 데이터 규결이 다르기 때문에 DTO를 다르게 해서 받아야함
        OAuth2Response oAuth2Response = null;
        if (registrationId.equals("naver")) {
            oAuth2Response = new NaverResponse(oAuth2User.getAttributes());
        } else if (registrationId.equals("google")) {
            oAuth2Response = new GoogleResponse(oAuth2User.getAttributes());
        }

        // 받은 정보를 DB에 저장
        String role = oAuth2UserDbService.saveOrUpdateUser(oAuth2Response);

        return new CustomOAuth2User(oAuth2Response, role);
    }
}
