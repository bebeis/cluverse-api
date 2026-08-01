package cluverse.place.repository.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LocalMapMarkerQueryResult(
        Long placeId,
        String name,
        String category,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        long recommendationCount,
        LocalDateTime lastRecommendedAt
) {
}
