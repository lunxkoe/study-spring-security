package com.study.controller;

import com.study.dto.JoinRequest;
import com.study.dto.LoginRequest;
import com.study.dto.LoginResponse;
import com.study.jwt.JWTUtil;
import com.study.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final JWTUtil jwtUtil;

    @PostMapping("/join")
    public String joinProcess(@RequestBody JoinRequest request) {
        authService.joinProcess(request);
        return "ok";
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse loginResponse = authService.loginProcess(request);

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set("Authorization", "Bearer " + loginResponse.token());

            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", "로그인 성공");
            responseBody.put("username", loginResponse.username());

            return new ResponseEntity<>(responseBody, httpHeaders, HttpStatus.OK);
        } catch (AuthenticationException e) {
            log.info("로그인 실패: {}", e.getMessage());

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "아이디 또는 비밀번호가 일치하지 않습니다.");

            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }
    }
}
