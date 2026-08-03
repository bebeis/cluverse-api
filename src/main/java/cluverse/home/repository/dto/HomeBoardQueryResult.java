package cluverse.home.repository.dto;

import cluverse.board.domain.BoardType;

public record HomeBoardQueryResult(
        Long boardId,
        BoardType boardType,
        String name,
        Long parentBoardId
) {
}
