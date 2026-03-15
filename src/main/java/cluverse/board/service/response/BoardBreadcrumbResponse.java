package cluverse.board.service.response;

public record BoardBreadcrumbResponse(
        Long boardId,
        String name,
        int depth
) {
}
