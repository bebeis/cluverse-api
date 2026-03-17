package cluverse.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthExceptionMessage {

    UNAUTHORIZED("인증이 필요합니다."),
    INVALID_CREDENTIALS("이메일 또는 비밀번호가 올바르지 않습니다."),
    UNSUPPORTED_OAUTH_PROVIDER("지원하지 않는 OAuth2 provider입니다."),
    EMAIL_ALREADY_EXISTS("이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS("이미 사용 중인 닉네임입니다."),
    SOCIAL_NICKNAME_GENERATION_FAILED("소셜 회원 닉네임 생성에 실패했습니다."),
    UNIVERSITY_NOT_FOUND("존재하지 않는 학교입니다."),
    REQUIRED_TERMS_NOT_AGREED("필수 약관에 동의해주세요."),
    INVALID_OAUTH_TOKEN("유효하지 않거나 만료된 OAuth 인증 토큰입니다.");

    private final String message;
}
