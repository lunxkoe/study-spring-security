package com.example.oauthjwt.jwt;

import com.example.oauthjwt.dto.CustomOAuth2User;
import com.example.oauthjwt.dto.UserDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // JWT 토큰
        String authorization = null;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                log.info(cookie.getName());
                if (cookie.getName().equals("Authorization")) {
                    authorization = cookie.getValue();
                }
            }
        }

        if (authorization == null) {
            log.info("token null");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization;

        if (jwtUtil.isExpired(token)) {
            log.info("Token is expired");
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtUtil.getUsername(token);
        String role = jwtUtil.getRole(token);


        UserDto userDto = new UserDto();
        userDto.setUsername(username);
        userDto.setRole(role);

        // UserDetails에 회원 정보 객체 담기
        CustomOAuth2User customOAuth2User = new CustomOAuth2User(userDto);

        // 스프링 시큐리티 인증 토큰 생성
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(customOAuth2User, null, customOAuth2User.getAuthorities());

        // 세션에 사용자 등록
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // 다음 요청으로 넘어가기
        filterChain.doFilter(request, response);
    }
}
