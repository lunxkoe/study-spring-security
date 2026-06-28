package com.example.springjwt.controller;

import com.example.springjwt.dto.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    @GetMapping("/admin")
    public String adminP(@AuthenticationPrincipal CustomUserDetails userDetails) {

        return "admin controller" + userDetails.getUsername();
    }
}
