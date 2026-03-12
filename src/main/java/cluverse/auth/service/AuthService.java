package cluverse.auth.service;

import cluverse.auth.client.OAuthUserInfo;
import cluverse.auth.service.implement.AuthReader;
import cluverse.auth.service.implement.AuthWriter;
import cluverse.auth.service.request.MemberRegisterRequest;
import cluverse.common.auth.LoginMember;
import cluverse.common.auth.LoginMemberArgumentResolver;
import cluverse.member.domain.Member;
import cluverse.member.domain.OAuthProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final AuthReader authReader;
    private final AuthWriter authWriter;

    public LoginMember register(MemberRegisterRequest request, String clientIp, HttpServletRequest httpRequest) {
        Member member = authWriter.register(request);
        authWriter.updateLastLogin(member, clientIp);
        LoginMember loginMember = LoginMember.from(member);
        createSession(httpRequest, loginMember);
        return loginMember;
    }

    public LoginMember loginWithOAuth(OAuthUserInfo userInfo, OAuthProvider provider, String clientIp, HttpServletRequest request) {
        Member member = authReader.findBySocialAccount(provider, userInfo.providerId())
                .orElseGet(() -> authWriter.registerBySocial(userInfo, provider));
        authWriter.updateLastLogin(member, clientIp);
        LoginMember loginMember = LoginMember.from(member);
        createSession(request, loginMember);
        return loginMember;
    }

    public LoginMember loginWithEmail(String email, String rawPassword, String clientIp, HttpServletRequest request) {
        Member member = authReader.readByEmailAndPassword(email, rawPassword);
        authWriter.updateLastLogin(member, clientIp);
        LoginMember loginMember = LoginMember.from(member);
        createSession(request, loginMember);
        return loginMember;
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private void createSession(HttpServletRequest request, LoginMember loginMember) {
        HttpSession session = request.getSession(true);
        session.setAttribute(LoginMemberArgumentResolver.SESSION_KEY, loginMember);
    }
}
