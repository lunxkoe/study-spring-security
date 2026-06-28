package study.oidc.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.oidc.oidc.CustomOidcUser;

@RestController
public class MainController {

    @GetMapping("/")
    public String index(@AuthenticationPrincipal CustomOidcUser oidcUser) {
//        String username = SecurityContextHolder.getContext().getAuthentication().getName();
//        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().toString();

//        String username = oidcUser.getEmail();
//        String role = oidcUser.getAuthorities().iterator().next().toString();

        String username = oidcUser.getNickname();
        String role = oidcUser.getAuthorities().iterator().next().toString();

        return username + " : " + role;
    }
}
