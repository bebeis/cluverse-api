package cluverse.place.service;

import cluverse.place.domain.Place;
import cluverse.place.repository.dto.PlaceContentQueryResult;
import cluverse.place.service.implement.LocalMapReader;
import cluverse.place.service.implement.PlaceContentCursorCodec;
import cluverse.place.service.implement.PlaceReader;
import cluverse.place.service.response.PlaceContentResponse;
import cluverse.place.service.response.PlaceContentsResponse;
import cluverse.place.service.response.PlaceDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceQueryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final PlaceReader placeReader;
    private final LocalMapReader localMapReader;
    private final PlaceContentCursorCodec cursorCodec;

    public PlaceDetailResponse readDetail(Long placeId) {
        Place place = placeReader.readOrThrow(placeId);
        return PlaceDetailResponse.of(place, localMapReader.countRecommendations(placeId));
    }

    public PlaceContentsResponse readContents(Long placeId, String cursor, Integer requestedSize) {
        placeReader.readOrThrow(placeId);
        int size = normalizeSize(requestedSize);
        PlaceContentCursorCodec.Cursor decoded = cursorCodec.decode(cursor);
        List<PlaceContentQueryResult> rows = localMapReader.readContents(
                placeId, decoded.createdAt(), decoded.contentType(), decoded.contentId(), size + 1);
        boolean hasNext = rows.size() > size;
        List<PlaceContentQueryResult> page = hasNext ? rows.subList(0, size) : rows;
        String nextCursor = hasNext && !page.isEmpty() ? cursorCodec.encode(page.getLast()) : null;
        return new PlaceContentsResponse(
                page.stream().map(PlaceContentResponse::from).toList(), nextCursor, hasNext);
    }

    private int normalizeSize(Integer requestedSize) {
        if (requestedSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.max(1, Math.min(requestedSize, MAX_PAGE_SIZE));
    }
}
