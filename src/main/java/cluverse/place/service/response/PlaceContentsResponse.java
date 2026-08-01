package cluverse.place.service.response;

import java.util.List;

public record PlaceContentsResponse(
        List<PlaceContentResponse> contents,
        String nextCursor,
        boolean hasNext
) {
    public PlaceContentsResponse {
        contents = List.copyOf(contents);
    }
}
