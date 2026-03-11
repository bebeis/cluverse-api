package cluverse.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthExceptionMessage {

    UNAUTHORIZED("인증이 필요합니다."),
    INVALID_CREDENTIALS("이메일 또는 비밀번호가 올바르지 않습니다."),
    UNSUPPORTED_OAUTH_PROVIDER("지원하지 않는 OAuth2 provider입니다.");

    private final String message;
}
