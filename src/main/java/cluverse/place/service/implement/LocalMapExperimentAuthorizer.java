package cluverse.place.service.implement;

import cluverse.common.exception.ForbiddenException;
import cluverse.place.exception.PlaceExceptionMessage;
import cluverse.place.properties.LocalMapProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class LocalMapExperimentAuthorizer {

    private final LocalMapProperties properties;

    public void authorize(String token) {
        if (!properties.experimentEndpointsEnabled()) {
            throw new ForbiddenException(PlaceExceptionMessage.LOCAL_MAP_EXPERIMENT_DISABLED.getMessage());
        }
        byte[] expected = properties.benchmarkToken().getBytes(StandardCharsets.UTF_8);
        byte[] actual = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
        if (expected.length == 0 || !MessageDigest.isEqual(expected, actual)) {
            throw new ForbiddenException(PlaceExceptionMessage.INVALID_BENCHMARK_TOKEN.getMessage());
        }
    }
}
