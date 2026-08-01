package cluverse.place.service.response;

import cluverse.place.domain.PlaceCategory;
import cluverse.place.repository.dto.LocalMapMarkerQueryResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LocalMapMarkerResponse(
        Long placeId,
        String name,
        PlaceCategory category,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        long recommendationCount,
        LocalDateTime lastRecommendedAt
) {
    public static LocalMapMarkerResponse from(LocalMapMarkerQueryResult result) {
        return new LocalMapMarkerResponse(
                result.placeId(), result.name(), PlaceCategory.valueOf(result.category()), result.address(),
                result.roadAddress(), result.latitude(), result.longitude(), result.recommendationCount(),
                result.lastRecommendedAt()
        );
    }
}
