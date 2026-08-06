package cluverse.place.service;

import cluverse.place.client.StubPlaceSearchClient;
import cluverse.place.properties.LocalMapProperties;
import cluverse.place.properties.PlaceProviderMode;
import cluverse.place.service.response.LocalMapBenchmarkReadinessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocalMapBenchmarkService {

    private final LocalMapProperties properties;
    private final Optional<StubPlaceSearchClient> stubPlaceSearchClient;

    public LocalMapBenchmarkReadinessResponse readReadiness() {
        boolean stubProvider = properties.providerMode() == PlaceProviderMode.STUB
                && stubPlaceSearchClient.isPresent();
        long delayMillis = stubPlaceSearchClient.map(StubPlaceSearchClient::delayMillis).orElse(-1L);
        long searchCalls = stubPlaceSearchClient.map(StubPlaceSearchClient::searchCalls).orElse(-1L);
        return new LocalMapBenchmarkReadinessResponse(
                properties.providerMode(), properties.experimentEndpointsEnabled(), stubProvider,
                delayMillis, searchCalls);
    }

    public LocalMapBenchmarkReadinessResponse resetStub(long delayMillis) {
        StubPlaceSearchClient stub = stubPlaceSearchClient.orElseThrow(() ->
                new IllegalStateException("provider mock이 활성화되어 있지 않습니다."));
        stub.reset(delayMillis);
        return readReadiness();
    }
}
