package cluverse.board.service.response;

import cluverse.board.domain.BoardType;

import java.util.List;

public record BoardDirectoryResponse(
        BoardType boardType,
        Long parentBoardId,
        int requestedDepth,
        boolean activeOnly,
        List<BoardSummaryResponse> boards
) {
    public BoardDirectoryResponse {
        boards = boards == null ? List.of() : List.copyOf(boards);
    }
}
