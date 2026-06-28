package study.security6.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    // 이게 반드시 필요함!!
    @Column(nullable = false)
    private String role;

    private User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public static User create(String username, String password, String role) {
        return new User(username, password, role);
    }
}
