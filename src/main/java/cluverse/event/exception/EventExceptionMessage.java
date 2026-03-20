package cluverse.event.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventExceptionMessage {
    EVENT_NOT_FOUND("존재하지 않는 행사입니다.");

    private final String message;
}
