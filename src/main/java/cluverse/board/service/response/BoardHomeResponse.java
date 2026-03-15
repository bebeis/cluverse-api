package cluverse.board.service.response;

import cluverse.board.domain.BoardType;

import java.util.List;

public record BoardHomeResponse(
        Long boardId,
        BoardType boardType,
        String name,
        String description,
        boolean isMemberOnly,
        boolean isReadable,
        boolean isWritable,
        boolean isManageable,
        boolean isExternalVisible,
        BoardHomeTabType defaultTab,
        List<BoardHomeTabResponse> tabs,
        List<BoardSortOption> supportedSorts,
        BoardPostingPolicyResponse postingPolicy
) {
    public BoardHomeResponse {
        tabs = tabs == null ? List.of() : List.copyOf(tabs);
        supportedSorts = supportedSorts == null ? List.of() : List.copyOf(supportedSorts);
    }
}
