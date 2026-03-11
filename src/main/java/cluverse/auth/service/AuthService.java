package cluverse.auth.service;

import cluverse.auth.client.OAuthUserInfo;
import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.auth.service.request.MemberRegisterRequest;
import cluverse.common.auth.LoginMember;
import cluverse.common.auth.LoginMemberArgumentResolver;
import cluverse.common.config.PasswordConfig;
import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.NotFoundException;
import cluverse.common.exception.UnauthorizedException;
import cluverse.member.domain.Member;
import cluverse.member.domain.MemberProfile;
import cluverse.member.domain.MemberTermsAgreement;
import cluverse.member.domain.OAuthProvider;
import cluverse.member.repository.MemberQueryRepository;
import cluverse.member.repository.MemberRepository;
import cluverse.member.repository.MemberTermsAgreementRepository;
import cluverse.member.repository.TermsRepository;
import cluverse.university.repository.UniversityRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final MemberRepository memberRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final TermsRepository termsRepository;
    private final MemberTermsAgreementRepository memberTermsAgreementRepository;
    private final UniversityRepository universityRepository;
    private final PasswordConfig passwordConfig;

    public LoginMember register(MemberRegisterRequest request, String clientIp, HttpServletRequest httpRequest) {
        validateEmailNotDuplicated(request.email());
        validateNicknameNotDuplicated(request.nickname());
        validateUniversityExists(request.universityId());
        validateRequiredTermsAgreed(request.agreedTermsIds());

        String passwordHash = passwordConfig.encode(request.password());
        Member member = Member.create(request.nickname(), request.universityId());
        member.initMemberAuth(request.email(), passwordHash);

        MemberProfile profile = MemberProfile.create(member);
        member.initProfile(profile);

        memberRepository.save(member);

        request.agreedTermsIds().forEach(termsId ->
                memberTermsAgreementRepository.save(MemberTermsAgreement.of(member, termsId))
        );

        member.updateLastLogin(clientIp);

        LoginMember loginMember = LoginMember.from(member);
        createSession(httpRequest, loginMember);
        return loginMember;
    }

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

    private void validateEmailNotDuplicated(String email) {
        if (memberQueryRepository.existsByEmail(email)) {
            throw new BadRequestException(AuthExceptionMessage.EMAIL_ALREADY_EXISTS.getMessage());
        }
    }

    private void validateNicknameNotDuplicated(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new BadRequestException(AuthExceptionMessage.NICKNAME_ALREADY_EXISTS.getMessage());
        }
    }

    private void validateUniversityExists(Long universityId) {
        if (!universityRepository.existsById(universityId)) {
            throw new NotFoundException(AuthExceptionMessage.UNIVERSITY_NOT_FOUND.getMessage());
        }
    }

    private void validateRequiredTermsAgreed(List<Long> agreedTermsIds) {
        List<Long> requiredTermsIds = termsRepository.findAllByIsActiveTrueAndIsRequiredTrue()
                .stream().map(t -> t.getId()).toList();
        if (!agreedTermsIds.containsAll(requiredTermsIds)) {
            throw new BadRequestException(AuthExceptionMessage.REQUIRED_TERMS_NOT_AGREED.getMessage());
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
        Member member = Member.create(nickname, 0L);
        member.initMemberAuthBySocial(userInfo.email());
        member.addSocialAccount(provider, userInfo.providerId());

        MemberProfile profile = MemberProfile.create(member);
        member.initProfile(profile);

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
