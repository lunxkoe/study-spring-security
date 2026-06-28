package study.security6.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import study.security6.dto.JoinDto;
import study.security6.entity.User;
import study.security6.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class JoinService {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserRepository userRepository;

    @Transactional
    public void joinProcess(JoinDto joinDto) {

        // 가입 불가 문자 정규식 처리를 해주어야함!!

        // DB에 이미 동일한 username을 가진 회원이 존재하는지 반드시 검증해야함
        if (userRepository.existsByUsername(joinDto.getUsername())) {
            return;
        }

        // 비밀번호 암호화
        String encodedPassword = bCryptPasswordEncoder.encode(joinDto.getPassword());

        // 신규 사용자 등록
        User newUser = User.create(
                joinDto.getUsername(),
                bCryptPasswordEncoder.encode(joinDto.getPassword()),
                "ROLE_ADMIN"
        );
        // - role은 무조건 ROLE_을 붙여함

        userRepository.save(newUser);
    }
}
