package cluverse.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonExceptionMessage {

    REQUIRED_VALUE_MISSING("필수 값은 비어 있을 수 없습니다.");

    private final String message;
}
