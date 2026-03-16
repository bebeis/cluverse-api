package cluverse.feed.service.response;

import cluverse.board.domain.BoardType;
import cluverse.feed.repository.dto.FeedPostQueryDto;

public record FeedBoardResponse(
        Long boardId,
        BoardType boardType,
        String name,
        Long parentBoardId
) {
    public static FeedBoardResponse from(FeedPostQueryDto post) {
        return new FeedBoardResponse(
                post.boardId(),
                post.boardType(),
                post.boardName(),
                post.parentBoardId()
        );
    }
}
