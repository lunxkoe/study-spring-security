package study.oidc.oidc;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Collection;

public class CustomOidcUser extends DefaultOidcUser {

    private final Long memberId;
    private final String nickname;

    public CustomOidcUser(
            Collection<? extends GrantedAuthority> authorities,
            OidcIdToken idToken,
            OidcUserInfo userInfo,
            Long memberId,
            String nickname
    ) {
        super(authorities, idToken, userInfo);
        this.memberId = memberId;
        this.nickname = nickname;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getNickname() {
        return nickname;
    }
}
