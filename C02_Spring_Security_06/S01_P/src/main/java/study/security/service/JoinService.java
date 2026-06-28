package study.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import study.security.dto.JoinRequest;
import study.security.entity.User;
import study.security.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class JoinService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Transactional
    public void joinProcess(JoinRequest request) {
        // 사용자명 중복 검사
        if (userRepository.existsByUsername(request.username())) {
            return;
        }

        // 사용자 생성
        User newUser = User.createUser(
                request.username(),
                passwordEncoder.encode(request.password())
        );

        // 사용자 저장
        userRepository.save(newUser);
    }
}
