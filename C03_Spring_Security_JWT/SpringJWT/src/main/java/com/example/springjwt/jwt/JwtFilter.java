package com.example.springjwt.jwt;

import com.example.springjwt.dto.CustomUserDetails;
import com.example.springjwt.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.info("[JWTFILTER]: token null");
            filterChain.doFilter(request, response);
            return; // 반드시 종료해야함!!
        }

        String token = authorization.split(" ")[1];

        // 토큰 만료 시간 검증
        if (jwtUtil.isExpired(token)) {
            log.info("[JWRFILTER]: token expired");
            return;
        }

        String username = jwtUtil.getUsername(token);
        String role = jwtUtil.getRole(token);

        // 인증은 통과를 했기 때문에 가짜 객체를 만들어서 그냥 참고용 객체를 만드는 과정이라고 생각
        User user = new User(username, "testpassword", role);

        // UserDetails에 회원 정보 객체 담기
        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        // 스프링 시큐리티 인증 토큰 생성
        // - new token(아이디, 비밀번호): 인증을 시도하기 전의 임시 토큰: 내부적으로 isAuthenticated = flase
        // - new token (주체, 비밀번호, 권한): 인증이 완벽한 신분증, isAuthenticated = true로 설정됨
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());

        // 세션에 사용자 등록 - 요청이 종료되면 없어짐
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
