package cluverse.member.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberExceptionMessage {

    MEMBER_NOT_FOUND("존재하지 않는 회원입니다."),
    UNIVERSITY_NOT_FOUND("존재하지 않는 학교입니다."),
    MAJOR_NOT_FOUND("존재하지 않는 학과입니다."),
    INTEREST_NOT_FOUND("존재하지 않는 관심 태그입니다."),
    ALREADY_FOLLOWING("이미 팔로우한 회원입니다."),
    NOT_FOLLOWING("팔로우하지 않은 회원입니다."),
    CANNOT_FOLLOW_SELF("자기 자신을 팔로우할 수 없습니다."),
    ALREADY_BLOCKED("이미 차단한 회원입니다."),
    NOT_BLOCKED("차단하지 않은 회원입니다."),
    CANNOT_BLOCK_SELF("자기 자신을 차단할 수 없습니다."),
    MAJOR_ALREADY_REGISTERED("이미 등록된 학과입니다."),
    INTEREST_ALREADY_REGISTERED("이미 등록된 관심 태그입니다."),
    PRIMARY_MAJOR_REQUIRED("주전공은 반드시 1개 있어야 합니다."),
    INVALID_PROFILE_VISIBLE_FIELD("유효하지 않은 프로필 공개 필드입니다.");

    private final String message;
}
