package cluverse.auth.service;

import cluverse.auth.client.OAuthUserInfo;
import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.auth.service.implement.AuthReader;
import cluverse.auth.service.implement.AuthWriter;
import cluverse.auth.service.request.MemberRegisterRequest;
import cluverse.common.auth.LoginMember;
import cluverse.common.exception.UnauthorizedException;
import cluverse.member.domain.Member;
import cluverse.member.domain.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final AuthReader authReader;
    private final AuthWriter authWriter;
    private final OAuthTokenStore oAuthTokenStore;

    public LoginMember register(MemberRegisterRequest request, String clientIp) {
        Member member = authWriter.register(request);
        return login(member, clientIp);
    }

    public String loginWithOAuthAndCreateToken(OAuthUserInfo userInfo, OAuthProvider provider, String clientIp) {
        Member member = authReader.findBySocialAccount(provider, userInfo.providerId())
                .orElseGet(() -> authWriter.registerBySocial(userInfo, provider));
        LoginMember loginMember = login(member, clientIp);
        return oAuthTokenStore.save(loginMember);
    }

    public LoginMember exchangeOAuthToken(String token) {
        LoginMember loginMember = oAuthTokenStore.consume(token);
        if (loginMember == null) {
            throw new UnauthorizedException(AuthExceptionMessage.INVALID_OAUTH_TOKEN.getMessage());
        }
        return loginMember;
    }

    public LoginMember loginWithEmail(String email, String rawPassword, String clientIp) {
        Member member = authReader.readByEmailAndPassword(email, rawPassword);
        return login(member, clientIp);
    }

    private LoginMember login(Member member, String clientIp) {
        authWriter.updateLastLogin(member, clientIp);
        return LoginMember.from(member);
    }
}
