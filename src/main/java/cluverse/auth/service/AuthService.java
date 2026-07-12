package cluverse.auth.service;

import cluverse.auth.client.OAuthUserInfo;
import cluverse.auth.service.implement.AuthProcessor;
import cluverse.auth.service.request.MemberRegisterRequest;
import cluverse.common.auth.LoginMember;
import cluverse.member.domain.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthProcessor authProcessor;

    public LoginMember register(MemberRegisterRequest request, String clientIp) {
        return LoginMember.from(authProcessor.register(request, clientIp));
    }

    public LoginMember loginWithOAuth(OAuthUserInfo userInfo, OAuthProvider provider, String clientIp) {
        return LoginMember.from(authProcessor.loginWithOAuth(userInfo, provider, clientIp));
    }

    public LoginMember loginWithEmail(String email, String rawPassword, String clientIp) {
        return LoginMember.from(authProcessor.loginWithEmail(email, rawPassword, clientIp));
    }
}
