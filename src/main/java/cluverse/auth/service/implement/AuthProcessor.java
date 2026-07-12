package cluverse.auth.service.implement;

import cluverse.auth.client.OAuthUserInfo;
import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.auth.service.request.MemberRegisterRequest;
import cluverse.common.exception.UnauthorizedException;
import cluverse.member.domain.Member;
import cluverse.member.domain.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class AuthProcessor {

    private final AuthReader authReader;
    private final AuthWriter authWriter;

    public Member register(MemberRegisterRequest request, String clientIp) {
        return prepareLogin(authWriter.register(request), clientIp);
    }

    public Member loginWithOAuth(OAuthUserInfo userInfo, OAuthProvider provider, String clientIp) {
        Member member = authReader.findBySocialAccount(provider, userInfo.providerId())
                .orElseGet(() -> authWriter.registerBySocial(userInfo, provider));
        return prepareLogin(member, clientIp);
    }

    public Member loginWithEmail(String email, String rawPassword, String clientIp) {
        return prepareLogin(authReader.readByEmailAndPassword(email, rawPassword), clientIp);
    }

    private Member prepareLogin(Member member, String clientIp) {
        validateActive(member);
        authWriter.updateLastLogin(member, clientIp);
        return member;
    }

    private void validateActive(Member member) {
        if (!member.isActive()) {
            throw new UnauthorizedException(AuthExceptionMessage.INVALID_CREDENTIALS.getMessage());
        }
    }
}
