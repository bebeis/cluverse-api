package cluverse.university.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UniversityExceptionMessage {

    UNIVERSITY_NOT_FOUND("존재하지 않는 학교입니다."),
    UNIVERSITY_NAME_ALREADY_EXISTS("이미 등록된 학교명입니다."),
    UNIVERSITY_ACCESS_DENIED("학교 관리 권한이 없습니다.");

    private final String message;
}
