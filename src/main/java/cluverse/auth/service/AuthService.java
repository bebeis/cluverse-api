package cluverse.auth.service;

import cluverse.auth.client.OAuthUserInfo;
import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.auth.service.implement.AuthReader;
import cluverse.auth.service.implement.AuthWriter;
import cluverse.common.exception.UnauthorizedException;
import cluverse.auth.service.request.MemberRegisterRequest;
import cluverse.common.auth.LoginMember;
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

    public LoginMember register(MemberRegisterRequest request, String clientIp) {
        Member member = authWriter.register(request);
        return login(member, clientIp);
    }

    public LoginMember loginWithOAuth(OAuthUserInfo userInfo, OAuthProvider provider, String clientIp) {
        Member member = authReader.findBySocialAccount(provider, userInfo.providerId())
                .orElseGet(() -> authWriter.registerBySocial(userInfo, provider));
        return login(member, clientIp);
    }

    public LoginMember loginWithEmail(String email, String rawPassword, String clientIp) {
        Member member = authReader.readByEmailAndPassword(email, rawPassword);
        return login(member, clientIp);
    }

    private LoginMember login(Member member, String clientIp) {
        validateActive(member);
        authWriter.updateLastLogin(member, clientIp);
        return LoginMember.from(member);
    }

    private void validateActive(Member member) {
        if (!member.isActive()) {
            throw new UnauthorizedException(AuthExceptionMessage.INVALID_CREDENTIALS.getMessage());
        }
    }
}
