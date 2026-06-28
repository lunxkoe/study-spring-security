package com.example.oauthjwt.oauth2;

import com.example.oauthjwt.dto.CustomOAuth2User;
import com.example.oauthjwt.jwt.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

@Configuration
@RequiredArgsConstructor
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        // 사용자 이름
        String username = customOAuth2User.getUsername();

        // 권한
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

        String token = jwtUtil.createJwt(username, role, 60 * 60 * 60L);

        // 쿠키로 생성된 토큰을 넘겨주어야함
        response.addCookie(createCookie("Authorization", token));

        // 리다이렉트 주소도 넘겨줌
        response.sendRedirect("http://localhost:8080/");
    }

    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(60*60*60);
        //cookie.setSecure(true); // https에서 사용 가능
        cookie.setPath("/");
        cookie.setHttpOnly(true); // 자바스크립트가 가져가지 못하게 설정

        return cookie;
    }
}

// 이후 SecurityConfig: .successHandler(customSuccessHandler) 등록