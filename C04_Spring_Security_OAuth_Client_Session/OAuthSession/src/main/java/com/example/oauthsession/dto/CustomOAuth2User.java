package com.example.oauthsession.dto;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2Response oAuth2Response;
    private final String role;

    // 로그인 시 가져오는 리소스 서버로부터 가져오는 모든 데이터
    // - 지금은 구글과 네이버가 가져오는 형식이 달라서 일단 만들지 않음
    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }

    // Role 값
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collection = new ArrayList<>();
        collection.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                return role;
            }
        });
        return collection;
    }

    // 핵심 수정 사항
    // - DB의 principal_name 컬럼에 들어갈 값
    // - 단순 이름말고, 제공자_고유식별자 형태로
    @Override
    public String getName() {
//        return oAuth2Response.getName();
        return oAuth2Response.getProvider() + "_" + oAuth2Response.getProviderId();
    }

    // Username을 강제로 만들어서 넣어주기
    // - username이라고 지칭할 만한 것이 없음
    // - 따라서 수동으로 만들어주어야 함
    public String getUsername() {
        return oAuth2Response.getName() + " " + oAuth2Response.getProviderId();
    }
}
