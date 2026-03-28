package cluverse.auth.service.implement;

import cluverse.auth.client.OAuthUserInfo;
import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.auth.service.request.MemberRegisterRequest;
import cluverse.common.config.PasswordConfig;
import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.NotFoundException;
import cluverse.member.domain.*;
import cluverse.member.service.implement.MemberReader;
import cluverse.member.service.implement.MemberTermsAgreementWriter;
import cluverse.member.service.implement.MemberWriter;
import cluverse.member.service.implement.TermsReader;
import cluverse.university.service.implement.UniversityReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class AuthWriter {

    private static final int NICKNAME_MAX_LENGTH = 50;
    private static final int SOCIAL_NICKNAME_SUFFIX_LENGTH = 8;
    private static final int MAX_SOCIAL_NICKNAME_RETRY_COUNT = 10;
    private static final int MAX_RANDOM_SOCIAL_NICKNAME_RETRY_COUNT = 100;
    private static final String SOCIAL_NICKNAME_SEPARATOR = "_";
    private static final String DEFAULT_SOCIAL_NICKNAME = "user";

    private final MemberReader memberReader;
    private final MemberWriter memberWriter;
    private final MemberTermsAgreementWriter memberTermsAgreementWriter;
    private final TermsReader termsReader;
    private final UniversityReader universityReader;
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

        memberWriter.save(member);
        request.agreedTermsIds().forEach(termsId ->
                memberTermsAgreementWriter.save(member, termsId)
        );
        return member;
    }

    public Member registerBySocial(OAuthUserInfo userInfo, OAuthProvider provider) {
        return memberReader.findByEmail(userInfo.email())
                .map(member -> linkSocialAccount(member, provider, userInfo.providerId()))
                .orElseGet(() -> createSocialMember(userInfo, provider));
    }

    public void updateLastLogin(Member member, String clientIp) {
        member.updateLastLogin(clientIp);
    }

    private void validateEmailNotDuplicated(String email) {
        if (memberReader.existsByEmail(email)) {
            throw new BadRequestException(AuthExceptionMessage.EMAIL_ALREADY_EXISTS.getMessage());
        }
    }

    private void validateNicknameNotDuplicated(String nickname) {
        if (memberReader.existsByNickname(nickname)) {
            throw new BadRequestException(AuthExceptionMessage.NICKNAME_ALREADY_EXISTS.getMessage());
        }
    }

    private void validateUniversityExists(Long universityId) {
        universityReader.readOrThrow(universityId);
    }

    private void validateRequiredTermsAgreed(List<Long> agreedTermsIds) {
        List<Long> requiredTermsIds = termsReader.readRequiredTermsIds();
        if (!new HashSet<>(agreedTermsIds).containsAll(requiredTermsIds)) {
            throw new BadRequestException(AuthExceptionMessage.REQUIRED_TERMS_NOT_AGREED.getMessage());
        }
    }

    private Member linkSocialAccount(Member member, OAuthProvider provider, String providerUserId) {
        if (hasSocialAccount(member, provider, providerUserId)) {
            ensureProfile(member);
            return member;
        }

        member.addSocialAccount(provider, providerUserId);
        ensureProfile(member);
        return memberWriter.save(member);
    }

    private Member createSocialMember(OAuthUserInfo userInfo, OAuthProvider provider) {
        String nickname = createAvailableSocialNickname(userInfo.nickname(), provider, userInfo.providerId());
        Member member = Member.createSocialMember(nickname);
        member.initMemberAuthBySocial(userInfo.email());
        member.addSocialAccount(provider, userInfo.providerId());
        ensureProfile(member);
        return memberWriter.save(member);
    }

    private boolean hasSocialAccount(Member member, OAuthProvider provider, String providerUserId) {
        return member.getSocialAccounts().stream()
                .anyMatch(account -> account.getProvider() == provider && account.getProviderUserId().equals(providerUserId));
    }

    private void ensureProfile(Member member) {
        if (member.getProfile() == null) {
            member.initProfile(MemberProfile.create(member));
        }
    }

    private String createAvailableSocialNickname(String base, OAuthProvider provider, String providerUserId) {
        for (int attempt = 0; attempt < MAX_SOCIAL_NICKNAME_RETRY_COUNT; attempt++) {
            String nickname = generateSocialNickname(base, provider, providerUserId, attempt);
            if (!memberReader.existsByNickname(nickname)) {
                return nickname;
            }
        }
        return generateRandomSocialNickname(base);
    }

    private String generateRandomSocialNickname(String base) {
        String normalizedBase = normalizeNicknameBase(base);
        for (int attempt = 0; attempt < MAX_RANDOM_SOCIAL_NICKNAME_RETRY_COUNT; attempt++) {
            String suffix = UUID.randomUUID().toString().replace("-", "")
                    .substring(0, SOCIAL_NICKNAME_SUFFIX_LENGTH);
            String nickname = joinSocialNickname(normalizedBase, suffix);
            if (!memberReader.existsByNickname(nickname)) {
                return nickname;
            }
        }
        throw new IllegalStateException(AuthExceptionMessage.SOCIAL_NICKNAME_GENERATION_FAILED.getMessage());
    }

    private String generateSocialNickname(String base, OAuthProvider provider, String providerUserId, int attempt) {
        String normalizedBase = normalizeNicknameBase(base);
        String suffix = createSocialNicknameSuffix(provider, providerUserId, attempt);
        return joinSocialNickname(normalizedBase, suffix);
    }

    private String joinSocialNickname(String normalizedBase, String suffix) {
        int maxBaseLength = NICKNAME_MAX_LENGTH - SOCIAL_NICKNAME_SEPARATOR.length() - suffix.length();
        String truncatedBase = normalizedBase.length() > maxBaseLength
                ? normalizedBase.substring(0, maxBaseLength)
                : normalizedBase;
        return truncatedBase + SOCIAL_NICKNAME_SEPARATOR + suffix;
    }

    private String normalizeNicknameBase(String base) {
        if (base == null || base.isBlank()) {
            return DEFAULT_SOCIAL_NICKNAME;
        }
        return base.trim();
    }

    private String createSocialNicknameSuffix(OAuthProvider provider, String providerUserId, int attempt) {
        String hashSource = provider.name() + ":" + providerUserId;
        if (attempt > 0) {
            hashSource = hashSource + ":" + attempt;
        }
        byte[] digest = sha256(hashSource);
        StringBuilder builder = new StringBuilder(SOCIAL_NICKNAME_SUFFIX_LENGTH);
        for (int i = 0; builder.length() < SOCIAL_NICKNAME_SUFFIX_LENGTH && i < digest.length; i++) {
            builder.append(String.format(Locale.ROOT, "%02x", digest[i]));
        }
        return builder.substring(0, SOCIAL_NICKNAME_SUFFIX_LENGTH);
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable.", e);
        }
    }
}
