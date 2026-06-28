package com.study.dto;

public record JoinRequest(
        String username,
        String nickname,
        String password
) {
}
