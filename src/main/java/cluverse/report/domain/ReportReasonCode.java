package cluverse.report.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReasonCode {
    SPAM("스팸/광고"),
    ABUSE("욕설/혐오 표현"),
    ADULT("성인/부적절한 콘텐츠"),
    FRAUD("사기/허위 정보"),
    COPYRIGHT("저작권 침해"),
    ETC("기타");

    private final String description;
}
