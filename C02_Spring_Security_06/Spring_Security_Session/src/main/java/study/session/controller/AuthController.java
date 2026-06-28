package study.session.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/join")
    public String joinP() {
        return "join";
    }

    @GetMapping("/login")
    public String loginP() {
        return "login";
    }
}
