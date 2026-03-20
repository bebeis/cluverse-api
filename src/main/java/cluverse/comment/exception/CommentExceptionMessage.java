package cluverse.comment.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommentExceptionMessage {

    COMMENT_NOT_FOUND("존재하지 않는 댓글입니다."),
    COMMENT_ACCESS_DENIED("댓글 삭제 권한이 없습니다."),
    COMMENT_UPDATE_ACCESS_DENIED("댓글 수정 권한이 없습니다."),
    COMMENT_MAX_DEPTH_EXCEEDED("댓글은 최대 5 depth까지 작성할 수 있습니다."),
    COMMENT_PARENT_POST_MISMATCH("부모 댓글이 요청한 게시글에 속하지 않습니다."),
    COMMENT_REPLY_NOT_ALLOWED("삭제되었거나 숨김 처리된 댓글에는 답글을 작성할 수 없습니다."),
    COMMENT_ALREADY_DELETED("이미 삭제된 댓글입니다."),
    COMMENT_LIKE_COUNT_ALREADY_ZERO("댓글 좋아요 수는 0보다 작아질 수 없습니다."),
    COMMENT_REPLY_COUNT_ALREADY_ZERO("대댓글 수는 0보다 작아질 수 없습니다.");

    private final String message;
}
