package cluverse.certification.service.implement;

import cluverse.certification.exception.CertificationExceptionMessage;
import cluverse.certification.properties.CertificationProperties;
import cluverse.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class CertificationExperimentAuthorizer {

    private final CertificationProperties properties;

    public void authorize(String token) {
        if (!properties.experimentEndpointsEnabled()) {
            throw new ForbiddenException(CertificationExceptionMessage.EXPERIMENT_DISABLED.getMessage());
        }
        byte[] expected = properties.benchmarkToken().getBytes(StandardCharsets.UTF_8);
        byte[] actual = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
        if (expected.length == 0 || !MessageDigest.isEqual(expected, actual)) {
            throw new ForbiddenException(CertificationExceptionMessage.INVALID_BENCHMARK_TOKEN.getMessage());
        }
    }
}
