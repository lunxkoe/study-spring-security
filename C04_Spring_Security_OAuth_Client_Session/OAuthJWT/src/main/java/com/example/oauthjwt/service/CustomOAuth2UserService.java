package com.example.oauthjwt.service;

import com.example.oauthjwt.dto.*;
import com.example.oauthjwt.entity.User;
import com.example.oauthjwt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("oAuth2User: {}", oAuth2User);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2Response oAuth2Response = null;
        if (registrationId.equals("naver")) {
            oAuth2Response = new NaverResponse(oAuth2User.getAttributes());
        } else if (registrationId.equals("google")) {
            oAuth2Response = new GoogleResponse(oAuth2User.getAttributes());
        } else {
            return null;
        }

        String username = oAuth2Response.getProvider() + "_" + oAuth2Response.getProviderId();
        String role = "ROLE_USER";
        User foundUser = userRepository.findByUsername(username);

        if (foundUser == null) {
            User newUser = new User(username, oAuth2Response.getName(), oAuth2Response.getEmail(), role);
            userRepository.save(newUser);

            UserDto userDto = new UserDto();
            userDto.setUsername(newUser.getUsername());
            userDto.setName(newUser.getName());
            userDto.setRole(newUser.getRole());

            return new CustomOAuth2User(userDto);
        } else {

            foundUser.updateUserInfo(username, oAuth2Response.getName(), oAuth2Response.getEmail());

            UserDto userDto = new UserDto();
            userDto.setUsername(foundUser.getUsername());
            userDto.setName(foundUser.getName());
            userDto.setRole(foundUser.getRole());

            return new CustomOAuth2User(userDto);
        }
    }
}
