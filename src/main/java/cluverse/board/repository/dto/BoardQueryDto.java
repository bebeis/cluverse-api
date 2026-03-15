package cluverse.board.repository.dto;

import cluverse.board.domain.BoardType;

public record BoardQueryDto(
        Long boardId,
        BoardType boardType,
        String name,
        String description,
        Long parentBoardId,
        int depth,
        int displayOrder,
        boolean isActive,
        long childCount
) {
}
