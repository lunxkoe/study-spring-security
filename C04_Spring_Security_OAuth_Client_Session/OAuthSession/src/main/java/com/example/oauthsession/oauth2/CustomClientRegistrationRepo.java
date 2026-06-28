package com.example.oauthsession.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
@RequiredArgsConstructor
public class CustomClientRegistrationRepo {

    private final SocialClientRegistration socialClientRegistration;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        // 방식 2가지
        // - 인메모리에 저장하는 방식: 최대 10개까지 밖에 안되서 메모리가 넘칠일이 없음(이걸 사용)
        // - Jdbc를 통해 DB에 저장하는 방식
        return new InMemoryClientRegistrationRepository(
                socialClientRegistration.naverClientRegistration(),
                socialClientRegistration.googleClientRegistration()
        );
    }
}
