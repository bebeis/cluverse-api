package cluverse.place.service.response;

import cluverse.place.properties.PlaceProviderMode;

public record LocalMapBenchmarkReadinessResponse(
        PlaceProviderMode providerMode,
        boolean experimentEndpointsEnabled,
        boolean stubProvider,
        long stubDelayMillis,
        long stubSearchCalls
) {
}
