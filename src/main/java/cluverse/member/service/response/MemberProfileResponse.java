package cluverse.member.service.response;

import cluverse.member.domain.Member;
import cluverse.member.domain.MemberProfileField;
import cluverse.member.domain.MemberProfile;
import cluverse.member.domain.VerificationStatus;

import java.util.List;

public record MemberProfileResponse(
        Long memberId,
        String nickname,
        MemberProfileSummaryResponse university,
        VerificationStatus verificationStatus,
        String bio,
        Integer entranceYear,
        String profileImageUrl,
        String linkGithub,
        String linkNotion,
        String linkPortfolio,
        String linkInstagram,
        String linkEtc,
        boolean isPublic,
        List<MemberProfileField> visibleFields,
        boolean isFollowing,
        boolean isBlocked,
        long followerCount,
        long followingCount,
        long postCount
) {
    public static MemberProfileResponse of(
            Member member,
            MemberProfile profile,
            MemberProfileSummaryResponse university,
            boolean isFollowing,
            boolean isBlocked,
            long followerCount,
            long followingCount,
            long postCount,
            boolean sameMember
    ) {
        return new MemberProfileResponse(
                member.getId(),
                member.getNickname(),
                getVisibleUniversity(profile, university, sameMember),
                member.getVerificationStatus(),
                getVisibleValue(profile, MemberProfileField.BIO, sameMember, MemberProfile::getBio),
                getVisibleValue(profile, MemberProfileField.ENTRANCE_YEAR, sameMember, MemberProfile::getEntranceYear),
                getVisibleValue(profile, MemberProfileField.PROFILE_IMAGE_URL, sameMember, MemberProfile::getProfileImageUrl),
                getVisibleValue(profile, MemberProfileField.LINK_GITHUB, sameMember, MemberProfile::getLinkGithub),
                getVisibleValue(profile, MemberProfileField.LINK_NOTION, sameMember, MemberProfile::getLinkNotion),
                getVisibleValue(profile, MemberProfileField.LINK_PORTFOLIO, sameMember, MemberProfile::getLinkPortfolio),
                getVisibleValue(profile, MemberProfileField.LINK_INSTAGRAM, sameMember, MemberProfile::getLinkInstagram),
                getVisibleValue(profile, MemberProfileField.LINK_ETC, sameMember, MemberProfile::getLinkEtc),
                profile == null || profile.isPublic(),
                profile == null ? List.of() : profile.getVisibleFields().stream().toList(),
                isFollowing,
                isBlocked,
                followerCount,
                followingCount,
                postCount
        );
    }

    private static MemberProfileSummaryResponse getVisibleUniversity(
            MemberProfile profile,
            MemberProfileSummaryResponse university,
            boolean sameMember
    ) {
        if (profile == null || profile.canView(MemberProfileField.UNIVERSITY, sameMember)) {
            return university;
        }
        return MemberProfileSummaryResponse.empty();
    }

    private static <T> T getVisibleValue(
            MemberProfile profile,
            MemberProfileField field,
            boolean sameMember,
            java.util.function.Function<MemberProfile, T> extractor
    ) {
        if (profile == null || !profile.canView(field, sameMember)) {
            return null;
        }
        return extractor.apply(profile);
    }
}
