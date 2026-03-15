package cluverse.board.service.response;

import cluverse.board.domain.Board;
import cluverse.board.domain.BoardType;

import java.time.LocalDateTime;

public record BoardAdminResponse(
        Long boardId,
        BoardType boardType,
        String name,
        String description,
        Long parentBoardId,
        int depth,
        int displayOrder,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BoardAdminResponse from(Board board) {
        return new BoardAdminResponse(
                board.getId(),
                board.getBoardType(),
                board.getName(),
                board.getDescription(),
                board.getParentId(),
                board.getDepth(),
                board.getDisplayOrder(),
                board.isActive(),
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
}
