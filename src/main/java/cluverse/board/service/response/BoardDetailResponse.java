package cluverse.board.service.response;

import cluverse.board.domain.BoardType;

import java.util.List;

public record BoardDetailResponse(
        Long boardId,
        BoardType boardType,
        String name,
        String description,
        Long parentBoardId,
        int depth,
        int displayOrder,
        boolean isActive,
        boolean isReadable,
        boolean isWritable,
        boolean isManageable,
        boolean isMemberOnly,
        BoardPostingPolicyResponse postingPolicy,
        List<BoardBreadcrumbResponse> breadcrumbs,
        List<BoardSummaryResponse> children,
        GroupBoardSummaryResponse group
) {
    public BoardDetailResponse {
        breadcrumbs = breadcrumbs == null ? List.of() : List.copyOf(breadcrumbs);
        children = children == null ? List.of() : List.copyOf(children);
    }
}
