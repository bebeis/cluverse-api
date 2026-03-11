package cluverse.common.auth;

import cluverse.member.domain.Member;
import cluverse.member.domain.MemberRole;

import java.io.Serializable;

public record LoginMember(
        Long memberId,
        String nickname,
        MemberRole role
) implements Serializable {

    public static LoginMember from(Member member) {
        return new LoginMember(member.getId(), member.getNickname(), member.getRole());
    }
}
