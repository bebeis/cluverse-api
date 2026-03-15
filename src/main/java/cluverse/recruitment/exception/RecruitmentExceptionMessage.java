package cluverse.recruitment.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecruitmentExceptionMessage {

    RECRUITMENT_NOT_FOUND("존재하지 않는 모집글입니다."),
    RECRUITMENT_ACCESS_DENIED("모집글 관리 권한이 없습니다."),
    RECRUITMENT_NOT_OPEN("현재 모집 중인 공고가 아닙니다."),
    RECRUITMENT_APPLICATION_NOT_FOUND("지원서를 찾을 수 없습니다."),
    RECRUITMENT_APPLICATION_ACCESS_DENIED("지원서 접근 권한이 없습니다."),
    RECRUITMENT_APPLICATION_ALREADY_EXISTS("이미 지원한 모집글입니다."),
    RECRUITMENT_APPLICATION_STATUS_INVALID("지원 상태를 변경할 수 없습니다.");

    private final String message;
}
