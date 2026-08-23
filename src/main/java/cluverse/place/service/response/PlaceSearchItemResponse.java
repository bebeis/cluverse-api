package cluverse.place.service.response;

import cluverse.place.domain.PlaceCandidate;
import cluverse.place.domain.PlaceCategory;

import java.math.BigDecimal;

public record PlaceSearchItemResponse(
        String name,
        PlaceCategory category,
        String rawCategory,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String sourceUrl,
        String selectionToken
) {

    public static PlaceSearchItemResponse of(PlaceCandidate candidate, String selectionToken) {
        return new PlaceSearchItemResponse(
                candidate.name(),
                candidate.category(),
                candidate.rawCategory(),
                candidate.address(),
                candidate.roadAddress(),
                candidate.latitude(),
                candidate.longitude(),
                candidate.sourceUrl(),
                selectionToken
        );
    }
}
