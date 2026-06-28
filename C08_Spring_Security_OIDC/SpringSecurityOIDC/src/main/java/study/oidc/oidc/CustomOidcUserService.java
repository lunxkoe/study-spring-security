package study.oidc.oidc;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class CustomOidcUserService extends OidcUserService {

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

        OidcUser oidcUser = super.loadUser(userRequest);

        System.out.println(oidcUser);
        System.out.println(oidcUser.getIdToken());
        System.out.println(oidcUser.getUserInfo());
        System.out.println(oidcUser.getClaims());

        // ROLE 만들어 넣기
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        String email = oidcUser.getEmail();
        Long customMemberId = 1004L;
        String customNickname = email.split("@")[0] + "천사";

//        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        return new CustomOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), customMemberId, customNickname);
    }
}
