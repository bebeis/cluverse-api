package cluverse.member.service.response;

public record MemberFollowResponse(
        Long memberId,
        String nickname,
        String profileImageUrl
) {
}
