package cluverse.post.service.implement;

import cluverse.common.exception.ForbiddenException;
import cluverse.post.properties.PostImageUploadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class PostImageUploadExperimentAuthorizer {

    private final PostImageUploadProperties properties;

    public void authorize(String token) {
        if (!properties.enabled()) {
            throw new ForbiddenException("이미지 업로드 실험 API가 비활성화되어 있습니다.");
        }
        byte[] expected = properties.benchmarkToken() == null
                ? new byte[0]
                : properties.benchmarkToken().getBytes(StandardCharsets.UTF_8);
        byte[] actual = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
        if (expected.length == 0 || !MessageDigest.isEqual(expected, actual)) {
            throw new ForbiddenException("벤치마크 토큰이 올바르지 않습니다.");
        }
        if (properties.lambdaFunctionName() == null || properties.lambdaFunctionName().isBlank()) {
            throw new ForbiddenException("외부 이미지 프로세서가 설정되지 않았습니다.");
        }
    }
}
