package cluverse.place.service.response;

import cluverse.place.domain.PlaceCandidate;
import cluverse.place.domain.PlaceCategory;

import java.math.BigDecimal;

public record PlaceSearchItemResponseV1(
        String query,
        String sourceFingerprint,
        String name,
        PlaceCategory category,
        String rawCategory,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String sourceUrl
) {

    public static PlaceSearchItemResponseV1 of(String query, PlaceCandidate candidate) {
        return new PlaceSearchItemResponseV1(
                query,
                candidate.sourceFingerprint(),
                candidate.name(),
                candidate.category(),
                candidate.rawCategory(),
                candidate.address(),
                candidate.roadAddress(),
                candidate.latitude(),
                candidate.longitude(),
                candidate.sourceUrl()
        );
    }
}
