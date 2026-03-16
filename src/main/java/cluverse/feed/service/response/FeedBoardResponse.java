package cluverse.feed.service.response;

import cluverse.board.domain.BoardType;

public record FeedBoardResponse(
        Long boardId,
        BoardType boardType,
        String name,
        Long parentBoardId
) {
}
