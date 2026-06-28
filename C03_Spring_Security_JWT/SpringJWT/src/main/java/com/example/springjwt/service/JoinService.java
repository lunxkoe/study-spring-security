package com.example.springjwt.service;

import com.example.springjwt.dto.JoinDto;
import com.example.springjwt.entity.User;
import com.example.springjwt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JoinService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Transactional
    public void joinProcess(JoinDto joinDto) {

        if (userRepository.existsByUsername(joinDto.getUsername())) {
            return;
        }

        User newUser = new User(
                joinDto.getUsername(),
                passwordEncoder.encode(joinDto.getPassword()),
                "ROLE_ADMIN"
        );

        userRepository.save(newUser);
    }
}
