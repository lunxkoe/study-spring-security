package com.study.service;

import com.study.dto.CustomUserDetails;
import com.study.dto.JoinRequest;
import com.study.dto.LoginRequest;
import com.study.dto.LoginResponse;
import com.study.entity.User;
import com.study.jwt.JWTUtil;
import com.study.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public void joinProcess(JoinRequest request) {
        // 아이디 중복 검증
        if (userRepository.existsByUsername(request.username())) {
            throw new RuntimeException("아이디가 중복입니다.");
        }

        User newUser = User.createAdmin(
                request.username(),
                request.nickname(),
                passwordEncoder.encode(request.password())
        );

        userRepository.save(newUser);
    }

    @Transactional
    public LoginResponse loginProcess(LoginRequest request) {

            // 인증 전 토큰 생성
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(request.username(), request.password());

            // AuthenticationManager에게 인증을 맡김 ??
            // - 내부적으로 만든 CustomUserDetailsService.loadUserByUsername을 사용해서 인증 수행
            Authentication authentication = authenticationManager.authenticate(authToken);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // JWT 발급을 위한 정보를 추출
            String username = userDetails.getUsername();
            String role = userDetails.getAuthorities().iterator().next().getAuthority();
            String nickname = userDetails.getUser().getNickname();

            // JWT 토큰 생성
            String token = jwtUtil.createJwt(username, nickname, role, 1000 * 60 * 60L);

            return new LoginResponse(token, username);
    }
}
