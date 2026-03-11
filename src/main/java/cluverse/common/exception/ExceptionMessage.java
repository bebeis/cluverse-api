package cluverse.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExceptionMessage {

    UNAUTHORIZED("인증이 필요합니다."),
    INVALID_CREDENTIALS("이메일 또는 비밀번호가 올바르지 않습니다.");

    private final String message;
}
