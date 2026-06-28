package com.example.springjwt.controller;

import com.example.springjwt.dto.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Iterator;

@RestController
public class MainController {

    @GetMapping("/")
    public String mainP() {
        // username 가져오기 (아이디)
        String name = SecurityContextHolder.getContext().getAuthentication().getName();

        // authentication에서 가져오는 방법
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // username 가져오기
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        // 권한 가져오는 방법
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

        return "main controller" + name + " " + username + " " + userDetails.getPassword() + " " + role;
    }
}
