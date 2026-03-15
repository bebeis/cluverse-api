package cluverse.board.service.response;

import cluverse.board.domain.BoardType;

public record BoardSummaryResponse(
        Long boardId,
        BoardType boardType,
        String name,
        String description,
        Long parentBoardId,
        int depth,
        int displayOrder,
        boolean isActive,
        long childCount,
        boolean isReadable,
        boolean isWritable,
        boolean isMemberOnly
) {
}
