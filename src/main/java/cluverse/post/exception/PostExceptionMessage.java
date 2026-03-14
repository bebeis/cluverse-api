package cluverse.post.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostExceptionMessage {

    POST_NOT_FOUND("존재하지 않는 게시글입니다."),
    POST_ACCESS_DENIED("게시글 수정/삭제 권한이 없습니다."),
    POST_LIKE_COUNT_ALREADY_ZERO("좋아요 수는 0보다 작아질 수 없습니다."),
    POST_BOOKMARK_COUNT_ALREADY_ZERO("북마크 수는 0보다 작아질 수 없습니다."),
    UNSUPPORTED_IMAGE_CONTENT_TYPE("지원하지 않는 이미지 형식입니다."),
    POST_PAGE_LIMIT_EXCEEDED("최대 500페이지까지 조회할 수 있습니다. 더 오래된 글은 날짜 기반 조회를 이용해주세요.");

    private final String message;
}
