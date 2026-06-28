package com.study.jwt;

import com.study.dto.CustomUserDetails;
import com.study.entity.User;
import com.study.entity.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
//@Component // 필터를 상속받고 컴포넌트로 등록하면 전역 필터로도 등록함
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.info("[JWTFilter]: token null");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.split(" ")[1];
        if (jwtUtil.isExpired(token)) {
            log.info("[JWTFilter]: token expired");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 상태 코드 설정
            response.getWriter().write("Token Expired"); // 에러 메시지 전송
            return;
        }

        String username = jwtUtil.getUsername(token);
        String role = jwtUtil.getRole(token);

        UserRole userRole = UserRole.valueOf(role.replace("ROLE_", ""));

        String nickname = jwtUtil.getNickname(token);

        // 인증은 통과를 했기 때문에 가짜 객체를 만들어서 그냥 참고용 객체를 만드는 과정
        User user = User.createTokenVerifyTmpUser(username, nickname, "", userRole);

        // UserDetails에 회원 정보를 넘기기
        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        // 스프링 시큐리티 인증 토큰 생성
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, "", customUserDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // JWT 검증을 완전히 건너뛸 주소들을 지정합니다.
        // 이 주소들로 들어오는 요청은 헤더에 만료된 토큰이 있든 없든 신경 쓰지 않고 그냥 통과시킵니다.
        return path.equals("/") || path.equals("/login") || path.equals("/join");
    }
}
