package cluverse.place.service.response;

import cluverse.place.domain.Place;
import cluverse.place.domain.PlaceCategory;
import cluverse.place.domain.PlaceProvider;

import java.math.BigDecimal;

public record PlaceDetailResponse(
        Long placeId,
        PlaceProvider provider,
        String name,
        PlaceCategory category,
        String rawCategory,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String sourceUrl,
        long recommendationCount
) {
    public static PlaceDetailResponse of(Place place, long recommendationCount) {
        return new PlaceDetailResponse(
                place.getId(), place.getProvider(), place.getName(), place.getCategory(), place.getRawCategory(),
                place.getAddress(), place.getRoadAddress(), place.getLatitude(), place.getLongitude(),
                place.getSourceUrl(), recommendationCount
        );
    }
}
