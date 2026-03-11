package cluverse.member.service.response;

import cluverse.member.domain.Member;
import cluverse.member.domain.MemberProfile;
import cluverse.member.domain.VerificationStatus;

public record MemberProfileResponse(
        Long memberId,
        String nickname,
        Long universityId,
        VerificationStatus verificationStatus,
        String bio,
        String profileImageUrl,
        String linkGithub,
        String linkNotion,
        String linkPortfolio,
        String linkInstagram,
        String linkEtc,
        boolean isPublic
) {
    public static MemberProfileResponse of(Member member, MemberProfile profile) {
        return new MemberProfileResponse(
                member.getId(),
                member.getNickname(),
                member.getUniversityId(),
                member.getVerificationStatus(),
                profile == null ? null : profile.getBio(),
                profile == null ? null : profile.getProfileImageUrl(),
                profile == null ? null : profile.getLinkGithub(),
                profile == null ? null : profile.getLinkNotion(),
                profile == null ? null : profile.getLinkPortfolio(),
                profile == null ? null : profile.getLinkInstagram(),
                profile == null ? null : profile.getLinkEtc(),
                profile == null || profile.isPublic()
        );
    }
}
