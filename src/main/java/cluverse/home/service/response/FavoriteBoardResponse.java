package cluverse.home.service.response;

import cluverse.board.domain.BoardType;
import cluverse.home.service.implement.FavoriteBoardView;

public record FavoriteBoardResponse(
        Long boardId,
        BoardType boardType,
        String name,
        Long parentBoardId
) {
    public static FavoriteBoardResponse from(FavoriteBoardView board) {
        return new FavoriteBoardResponse(
                board.boardId(), board.boardType(), board.name(), board.parentBoardId()
        );
    }
}
