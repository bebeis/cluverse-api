package cluverse.member.service.response;

import cluverse.member.domain.MemberInterest;

public record MemberInterestResponse(
        Long memberInterestId,
        Long interestId
) {
    public static MemberInterestResponse from(MemberInterest memberInterest) {
        return new MemberInterestResponse(
                memberInterest.getId(),
                memberInterest.getInterestId()
        );
    }
}
