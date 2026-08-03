package cluverse.certification.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CertificationExceptionMessage {
    SCHEDULE_UNAVAILABLE("자격시험 일정을 일시적으로 불러올 수 없습니다.");

    private final String message;
}
