package cluverse.place.domain;

import java.math.BigDecimal;

public record PlaceSourceCandidate(
        PlaceProvider provider,
        String name,
        String rawCategory,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String sourceUrl
) {
}
