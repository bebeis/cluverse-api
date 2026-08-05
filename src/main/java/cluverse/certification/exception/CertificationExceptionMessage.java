package cluverse.certification.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CertificationExceptionMessage {
    SCHEDULE_UNAVAILABLE("자격시험 일정을 일시적으로 불러올 수 없습니다."),
    EXPERIMENT_DISABLED("자격시험 실험 API가 비활성화되어 있습니다."),
    INVALID_BENCHMARK_TOKEN("벤치마크 토큰이 올바르지 않습니다.");

    private final String message;
}
