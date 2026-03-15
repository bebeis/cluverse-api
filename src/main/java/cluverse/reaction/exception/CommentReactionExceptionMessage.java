package cluverse.reaction.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommentReactionExceptionMessage {

    COMMENT_ALREADY_LIKED("이미 좋아요한 댓글입니다."),
    COMMENT_NOT_LIKED("좋아요하지 않은 댓글입니다.");

    private final String message;
}
