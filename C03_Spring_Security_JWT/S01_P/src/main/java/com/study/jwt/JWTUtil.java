package com.study.jwt;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JWTUtil {

    private final SecretKey secretKey;

    public JWTUtil(@Value("${spring.jwt.secret}") String secretKey) {
        this.secretKey = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm()
        );
    }

    // 토큰 생성 메소드
    public String createJwt(String username, String nickname, String role, Long expiredMs) {
        return Jwts.builder()
                .claim("username", username) // 페이로드에 들어갈 정보
                .claim("role", role)         // 페이로드에 들어갈 정보
                .claim("nickname", nickname)
                .issuedAt(new Date(System.currentTimeMillis())) // 발급 시간
                .expiration(new Date(System.currentTimeMillis() + expiredMs)) // 토큰 만료 시간
                .signWith(this.secretKey)          // 암호화 알고리즘 비밀키로 서명
                .compact();                        // 토큰을 문자열로 압축하여 반환
    }

    // 토큰에서 Username 추출 메서드
    public String getUsername(String token) {
        return Jwts.parser()
                .verifyWith(secretKey) // 내가 가진 비밀키로 서명이 유효한지 확인
                .build()
                .parseSignedClaims(token) // 토큰을 열어서
                .getPayload()
                .get("username", String.class); // username이라는 이름의 크레임을 꺼냄
    }

    // 토큰에서 Role 추출 메서드
    public String getRole(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    // 토큰에서 Nickname 추출 메서드
    public String getNickname(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("nickname", String.class);
    }

    // 토큰 만료 여부 확인 메서드
    public Boolean isExpired(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration()
                .before(new Date()); // 현재 시간보다 만료 시간이 이전인지 확인
    }
}
