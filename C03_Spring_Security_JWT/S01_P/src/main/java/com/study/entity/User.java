package com.study.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(unique = true)
    private String username;    // 아이디
    private String nickname;    // 닉네임
    private String password;    // 비밀번호
    private UserRole userRole;  // 권한

    private User(String username, String nickname, String password, UserRole userRole) {
        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.userRole = userRole;
    }

    public static User createTokenVerifyTmpUser(String username, String nickname, String password, UserRole userRole) {
        return new User(username, nickname, password, userRole);
    }

    public static User createUser(String username, String nickname, String password) {
        return new User(username, nickname, password, UserRole.USER);
    }

    public static User createAdmin(String username, String nickname, String password) {
        return new User(username, nickname, password, UserRole.ADMIN);
    }
}
