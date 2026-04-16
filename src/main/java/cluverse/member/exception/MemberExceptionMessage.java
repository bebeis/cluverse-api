package cluverse.member.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberExceptionMessage {

    MEMBER_NOT_FOUND("존재하지 않는 회원입니다."),
    UNIVERSITY_NOT_FOUND("존재하지 않는 학교입니다."),
    UNIVERSITY_REGISTRATION_REQUIRED("학교 등록을 먼저 진행해주세요."),
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
    NICKNAME_ALREADY_EXISTS("이미 사용 중인 닉네임입니다."),
    INVALID_PROFILE_VISIBLE_FIELD("유효하지 않은 프로필 공개 필드입니다."),
    INVALID_PASSWORD("현재 비밀번호가 올바르지 않습니다."),
    PASSWORD_CHANGE_NOT_ALLOWED("비밀번호를 변경할 수 없는 계정입니다."),
    STUDENT_VERIFICATION_ALREADY_APPROVED("이미 학생 인증이 완료되었습니다."),
    STUDENT_VERIFICATION_NOT_FOUND("존재하지 않는 학생 인증 정보입니다."),
    STUDENT_VERIFICATION_EMAIL_DOMAIN_REQUIRED("학교 이메일 도메인이 등록되지 않은 학교입니다."),
    STUDENT_VERIFICATION_EMAIL_DOMAIN_MISMATCH("학교 이메일 도메인이 일치하지 않습니다."),
    STUDENT_VERIFICATION_EMAIL_CHALLENGE_NOT_FOUND("존재하지 않는 이메일 인증 요청입니다."),
    STUDENT_VERIFICATION_EMAIL_CHALLENGE_REPLACED("최신 인증 코드로 다시 인증해주세요."),
    STUDENT_VERIFICATION_EMAIL_CHALLENGE_EXPIRED("만료된 이메일 인증 코드입니다."),
    STUDENT_VERIFICATION_EMAIL_CHALLENGE_INVALID_STATUS("처리할 수 없는 이메일 인증 요청입니다."),
    STUDENT_VERIFICATION_EMAIL_CODE_INVALID("인증 코드가 올바르지 않습니다."),
    STUDENT_VERIFICATION_MAIL_SENDER_NOT_CONFIGURED("메일 발송 설정이 필요합니다."),
    STUDENT_VERIFICATION_MAIL_SEND_FAILED("인증 메일 발송에 실패했습니다.");

    private final String message;
}
