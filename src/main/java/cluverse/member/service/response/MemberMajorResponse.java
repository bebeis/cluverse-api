package cluverse.member.service.response;

import cluverse.member.domain.MajorType;
import cluverse.member.domain.MemberMajor;

public record MemberMajorResponse(
        Long memberMajorId,
        Long majorId,
        MajorType majorType
) {
    public static MemberMajorResponse from(MemberMajor memberMajor) {
        return new MemberMajorResponse(
                memberMajor.getId(),
                memberMajor.getMajorId(),
                memberMajor.getMajorType()
        );
    }
}
