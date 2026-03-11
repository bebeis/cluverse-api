package cluverse.auth.service;

import cluverse.auth.client.OAuthUserInfo;
import cluverse.common.auth.LoginMember;
import cluverse.common.auth.LoginMemberArgumentResolver;
import cluverse.common.config.PasswordConfig;
import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.UnauthorizedException;
import cluverse.member.domain.Member;
import cluverse.member.domain.OAuthProvider;
import cluverse.member.repository.MemberQueryRepository;
import cluverse.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final MemberRepository memberRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final PasswordConfig passwordConfig;

    public LoginMember loginWithOAuth(OAuthUserInfo userInfo, OAuthProvider provider, String clientIp, HttpServletRequest request) {
        Member member = memberQueryRepository.findBySocialAccount(provider, userInfo.providerId())
                .orElseGet(() -> registerBySocial(userInfo, provider));

        member.updateLastLogin(clientIp);

        LoginMember loginMember = LoginMember.from(member);
        createSession(request, loginMember);
        return loginMember;
    }

    public LoginMember loginWithEmail(String email, String rawPassword, String clientIp, HttpServletRequest request) {
        Member member = findMemberByEmailOrThrow(email);
        validatePassword(rawPassword, member.getMemberAuth().getPasswordHash());

        member.updateLastLogin(clientIp);

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

    private Member findMemberByEmailOrThrow(String email) {
        return memberQueryRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException(AuthExceptionMessage.INVALID_CREDENTIALS.getMessage()));
    }

    private void validatePassword(String rawPassword, String hashedPassword) {
        if (!passwordConfig.matches(rawPassword, hashedPassword)) {
            throw new UnauthorizedException(AuthExceptionMessage.INVALID_CREDENTIALS.getMessage());
        }
    }

    private Member registerBySocial(OAuthUserInfo userInfo, OAuthProvider provider) {
        String nickname = generateUniqueNickname(userInfo.nickname());
        Member member = Member.create(nickname);
        member.initMemberAuthBySocial(userInfo.email());
        member.addSocialAccount(provider, userInfo.providerId());
        return memberRepository.save(member);
    }

    private String generateUniqueNickname(String base) {
        String nickname = base;
        int suffix = 1;
        while (memberRepository.existsByNickname(nickname)) {
            nickname = base + suffix++;
        }
        return nickname;
    }

    private void createSession(HttpServletRequest request, LoginMember loginMember) {
        HttpSession session = request.getSession(true);
        session.setAttribute(LoginMemberArgumentResolver.SESSION_KEY, loginMember);
    }
}
