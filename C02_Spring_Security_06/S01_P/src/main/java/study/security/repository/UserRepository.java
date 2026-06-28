package study.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.security.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    User findByUsername(String username);
}
