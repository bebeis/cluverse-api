package cluverse.reaction.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostReactionExceptionMessage {

    POST_ALREADY_LIKED("이미 좋아요한 게시글입니다."),
    POST_NOT_LIKED("좋아요하지 않은 게시글입니다."),
    POST_ALREADY_BOOKMARKED("이미 북마크한 게시글입니다."),
    POST_NOT_BOOKMARKED("북마크하지 않은 게시글입니다.");

    private final String message;
}
