package cluverse.place.service;

import cluverse.place.properties.LocalMapProperties;
import cluverse.place.properties.PlaceProviderMode;
import cluverse.place.service.response.LocalMapBenchmarkReadinessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class LocalMapBenchmarkService {

    private final LocalMapProperties properties;

    public LocalMapBenchmarkReadinessResponse readReadiness() {
        String host = URI.create(properties.providerBaseUrl()).getHost();
        boolean stubProvider = properties.providerMode() == PlaceProviderMode.STUB
                && host != null
                && !host.equalsIgnoreCase("openapi.naver.com");
        return new LocalMapBenchmarkReadinessResponse(
                properties.providerMode(), properties.experimentEndpointsEnabled(), stubProvider);
    }
}
