package cluverse.home.service.response;

import cluverse.home.service.implement.FavoriteBoardPageView;

import java.util.List;

public record FavoriteBoardPageResponse(
        List<FavoriteBoardResponse> boards,
        Long nextCursor,
        boolean hasNext
) {
    public FavoriteBoardPageResponse {
        boards = List.copyOf(boards);
    }

    public static FavoriteBoardPageResponse from(FavoriteBoardPageView page) {
        return new FavoriteBoardPageResponse(
                page.boards().stream().map(FavoriteBoardResponse::from).toList(),
                page.nextCursor(),
                page.hasNext()
        );
    }
}
