package cluverse.home.service.implement;

import java.util.List;

public record FavoriteBoardPageView(
        List<FavoriteBoardView> boards,
        Long nextCursor,
        boolean hasNext
) {
    public FavoriteBoardPageView {
        boards = List.copyOf(boards);
    }
}
