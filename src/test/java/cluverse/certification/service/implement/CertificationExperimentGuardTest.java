package cluverse.certification.service.implement;

import cluverse.certification.properties.CertificationProperties;
import cluverse.certification.properties.CertificationProviderMode;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CertificationExperimentGuardTest {

    @Test
    void 실제_공공_API에서는_실험_엔드포인트를_활성화할_수_없다() {
        CertificationExperimentGuard guard = new CertificationExperimentGuard(properties(
                CertificationProviderMode.DATA_GO_KR,
                "https://apis.data.go.kr",
                true
        ));

        assertThatThrownBy(guard::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("자격시험 실험 API는 STUB provider에서만 활성화할 수 있습니다.");
    }

    @Test
    void 실제_공공데이터포털_host를_스텁으로_표시해도_거부한다() {
        CertificationExperimentGuard guard = new CertificationExperimentGuard(properties(
                CertificationProviderMode.STUB,
                "https://apis.data.go.kr",
                true
        ));

        assertThatThrownBy(guard::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("자격시험 실험 API가 실제 공공데이터포털 host를 가리키고 있습니다.");
    }

    @Test
    void 로컬_스텁에서만_실험_엔드포인트를_활성화한다() {
        CertificationExperimentGuard guard = new CertificationExperimentGuard(properties(
                CertificationProviderMode.STUB,
                "http://127.0.0.1:19091",
                true
        ));

        assertThatCode(guard::afterPropertiesSet).doesNotThrowAnyException();
    }

    private CertificationProperties properties(
            CertificationProviderMode providerMode,
            String providerBaseUrl,
            boolean experimentEndpointsEnabled
    ) {
        return new CertificationProperties(
                providerMode,
                providerBaseUrl,
                "test-key",
                Duration.ofMillis(500),
                Duration.ofSeconds(2),
                Duration.ofHours(12),
                experimentEndpointsEnabled,
                "benchmark-token"
        );
    }
}
