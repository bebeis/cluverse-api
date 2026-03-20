package cluverse.post.service.response;

import cluverse.board.domain.BoardType;
import cluverse.post.repository.dto.PostDetailQueryDto;

public record PostBoardResponse(
        Long boardId,
        BoardType boardType,
        String name,
        Long parentBoardId
) {
    public static PostBoardResponse from(PostDetailQueryDto post) {
        return new PostBoardResponse(
                post.boardId(),
                post.boardType(),
                post.boardName(),
                post.parentBoardId()
        );
    }
}
