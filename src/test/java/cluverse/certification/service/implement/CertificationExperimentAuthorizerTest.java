package cluverse.certification.service.implement;

import cluverse.certification.properties.CertificationProperties;
import cluverse.certification.properties.CertificationProviderMode;
import cluverse.common.exception.ForbiddenException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CertificationExperimentAuthorizerTest {

    @Test
    void 실험이_비활성화되면_토큰이_일치해도_거부한다() {
        CertificationExperimentAuthorizer authorizer = new CertificationExperimentAuthorizer(properties(false));

        assertThatThrownBy(() -> authorizer.authorize("benchmark-token"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("자격시험 실험 API가 비활성화되어 있습니다.");
    }

    @Test
    void 활성화된_실험은_일치하는_토큰만_허용한다() {
        CertificationExperimentAuthorizer authorizer = new CertificationExperimentAuthorizer(properties(true));

        assertThatThrownBy(() -> authorizer.authorize("wrong-token"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("벤치마크 토큰이 올바르지 않습니다.");
        assertThatCode(() -> authorizer.authorize("benchmark-token")).doesNotThrowAnyException();
    }

    private CertificationProperties properties(boolean experimentEndpointsEnabled) {
        return new CertificationProperties(
                CertificationProviderMode.STUB,
                "http://127.0.0.1:19091",
                "test-key",
                Duration.ofMillis(500),
                Duration.ofSeconds(2),
                Duration.ofHours(12),
                experimentEndpointsEnabled,
                "benchmark-token"
        );
    }
}
