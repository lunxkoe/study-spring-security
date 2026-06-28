package study.security6.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.security6.entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {

    boolean existsByUsername(String username);

    User findByUsername(String username);
}
