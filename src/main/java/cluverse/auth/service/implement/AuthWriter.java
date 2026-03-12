package cluverse.auth.service.implement;

import cluverse.auth.client.OAuthUserInfo;
import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.auth.service.request.MemberRegisterRequest;
import cluverse.common.config.PasswordConfig;
import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.NotFoundException;
import cluverse.member.domain.Member;
import cluverse.member.domain.MemberProfile;
import cluverse.member.domain.MemberTermsAgreement;
import cluverse.member.domain.OAuthProvider;
import cluverse.member.repository.MemberQueryRepository;
import cluverse.member.repository.MemberRepository;
import cluverse.member.repository.MemberTermsAgreementRepository;
import cluverse.member.repository.TermsRepository;
import cluverse.university.repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
public class AuthWriter {

    private final MemberRepository memberRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final TermsRepository termsRepository;
    private final MemberTermsAgreementRepository memberTermsAgreementRepository;
    private final UniversityRepository universityRepository;
    private final PasswordConfig passwordConfig;

    public Member register(MemberRegisterRequest request) {
        validateEmailNotDuplicated(request.email());
        validateNicknameNotDuplicated(request.nickname());
        validateUniversityExists(request.universityId());
        validateRequiredTermsAgreed(request.agreedTermsIds());

        Member member = Member.create(request.nickname(), request.universityId());
        member.initMemberAuth(request.email(), passwordConfig.encode(request.password()));

        MemberProfile profile = MemberProfile.create(member);
        member.initProfile(profile);

        memberRepository.save(member);
        request.agreedTermsIds().forEach(termsId ->
                memberTermsAgreementRepository.save(MemberTermsAgreement.of(member, termsId))
        );
        return member;
    }

    public Member registerBySocial(OAuthUserInfo userInfo, OAuthProvider provider) {
        String nickname = generateUniqueNickname(userInfo.nickname());
        Member member = Member.create(nickname, 0L);
        member.initMemberAuthBySocial(userInfo.email());
        member.addSocialAccount(provider, userInfo.providerId());

        MemberProfile profile = MemberProfile.create(member);
        member.initProfile(profile);

        return memberRepository.save(member);
    }

    public void updateLastLogin(Member member, String clientIp) {
        member.updateLastLogin(clientIp);
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
        List<Long> requiredTermsIds = termsRepository.findAllByIsActiveTrueAndIsRequiredTrue().stream()
                .map(terms -> terms.getId())
                .toList();
        if (!agreedTermsIds.containsAll(requiredTermsIds)) {
            throw new BadRequestException(AuthExceptionMessage.REQUIRED_TERMS_NOT_AGREED.getMessage());
        }
    }

    private String generateUniqueNickname(String base) {
        String nickname = base;
        int suffix = 1;
        while (memberRepository.existsByNickname(nickname)) {
            nickname = base + suffix++;
        }
        return nickname;
    }
}
