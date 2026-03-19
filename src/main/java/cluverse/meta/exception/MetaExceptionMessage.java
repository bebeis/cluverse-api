package cluverse.meta.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MetaExceptionMessage {

    POST_LIKE_COUNT_ALREADY_ZERO("좋아요 수는 0보다 작아질 수 없습니다."),
    POST_BOOKMARK_COUNT_ALREADY_ZERO("북마크 수는 0보다 작아질 수 없습니다."),
    POST_COMMENT_COUNT_ALREADY_ZERO("댓글 수는 0보다 작아질 수 없습니다."),
    POST_VIEW_COUNT_V2_INCREASE_FAILED("낙관적 락 기반 조회수 증가 재시도에 실패했습니다.");

    private final String message;
}
