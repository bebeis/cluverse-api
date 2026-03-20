package cluverse.calendar.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CalendarExceptionMessage {
    EVENT_NOT_FOUND("존재하지 않는 일정입니다."),
    EVENT_ACCESS_DENIED("해당 일정에 접근할 수 없습니다."),
    INVALID_EVENT_PERIOD("일정 종료 시각은 시작 시각보다 빠를 수 없습니다.");

    private final String message;
}
