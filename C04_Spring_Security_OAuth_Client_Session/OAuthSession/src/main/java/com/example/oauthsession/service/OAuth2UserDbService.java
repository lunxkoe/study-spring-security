package com.example.oauthsession.service;

import com.example.oauthsession.dto.OAuth2Response;
import com.example.oauthsession.entity.User;
import com.example.oauthsession.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuth2UserDbService {

    private final UserRepository userRepository;

    @Transactional
    public String saveOrUpdateUser(OAuth2Response response) {
        String username = response.getProvider() + " " + response.getProviderId();
        User foundUser = userRepository.findByUsername(username);
        String role = "ROLE_USER";
        if (foundUser == null) {
            User newUser = new User(username, response.getEmail(), role);
            userRepository.save(newUser);
        } else {
            foundUser.changeEmail(response.getEmail());
            role = foundUser.getRole();
        }
        return role;
    }
}
