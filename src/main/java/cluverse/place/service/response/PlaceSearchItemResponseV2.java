package cluverse.place.service.response;

import cluverse.place.domain.PlaceCandidate;
import cluverse.place.domain.PlaceCategory;

import java.math.BigDecimal;

public record PlaceSearchItemResponseV2(
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

    public static PlaceSearchItemResponseV2 of(PlaceCandidate candidate, String selectionToken) {
        return new PlaceSearchItemResponseV2(
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
