package cluverse.place.domain;

import java.math.BigDecimal;

public record PlaceCandidate(
        PlaceProvider provider,
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
}
