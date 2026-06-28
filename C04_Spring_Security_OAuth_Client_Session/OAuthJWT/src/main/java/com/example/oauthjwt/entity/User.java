package com.example.oauthjwt.entity;

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

    private String username;
    private String name;
    private String email;
    private String role;

    public User(String username, String name, String email, String role) {
        this.username = username;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public void updateUserInfo(String username, String name, String email) {
        this.username = username;
        this.name = name;
        this.email = email;
    }
}
