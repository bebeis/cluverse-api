package cluverse.home.service.implement;

import cluverse.board.domain.BoardType;
import cluverse.home.repository.dto.HomeBoardQueryResult;

public record FavoriteBoardView(
        Long boardId,
        BoardType boardType,
        String name,
        Long parentBoardId
) {
    public static FavoriteBoardView from(HomeBoardQueryResult result) {
        return new FavoriteBoardView(
                result.boardId(), result.boardType(), result.name(), result.parentBoardId()
        );
    }
}
