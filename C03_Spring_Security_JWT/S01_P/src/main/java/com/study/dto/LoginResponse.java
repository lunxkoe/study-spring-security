package com.study.dto;

public record LoginResponse(
        String token,
        String username
) {
}
