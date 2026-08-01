package cluverse.popularity.service.implement;

import cluverse.common.exception.ForbiddenException;
import cluverse.popularity.properties.PopularityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class PopularityExperimentAuthorizer {

    private final PopularityProperties properties;

    public void authorize(String token) {
        String expected = properties.benchmarkToken();
        if (expected == null || expected.isBlank()) {
            throw new ForbiddenException("벤치마크 토큰이 설정되지 않았습니다.");
        }
        byte[] actualBytes = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actualBytes)) {
            throw new ForbiddenException("벤치마크 토큰이 올바르지 않습니다.");
        }
    }
}
