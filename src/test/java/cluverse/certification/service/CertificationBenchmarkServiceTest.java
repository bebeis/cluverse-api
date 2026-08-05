package cluverse.certification.service;

import cluverse.certification.properties.CertificationProperties;
import cluverse.certification.properties.CertificationProviderMode;
import cluverse.certification.service.implement.CertificationScheduleReader;
import cluverse.certification.service.response.CertificationBenchmarkReadinessResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CertificationBenchmarkServiceTest {

    @Test
    void 로컬_스텁_설정과_캐시_TTL을_readiness로_반환한다() {
        CertificationScheduleReader reader = mock(CertificationScheduleReader.class);
        CertificationBenchmarkService service = new CertificationBenchmarkService(properties(), reader);

        CertificationBenchmarkReadinessResponse response = service.readReadiness();

        assertThat(response.providerMode()).isEqualTo(CertificationProviderMode.STUB);
        assertThat(response.experimentEndpointsEnabled()).isTrue();
        assertThat(response.stubProvider()).isTrue();
        assertThat(response.cacheTtlMillis()).isEqualTo(Duration.ofHours(12).toMillis());
    }

    @Test
    void 캐시_초기화는_일정_Reader에_위임한다() {
        CertificationScheduleReader reader = mock(CertificationScheduleReader.class);
        CertificationBenchmarkService service = new CertificationBenchmarkService(properties(), reader);

        service.evictCache();

        verify(reader).evictAll();
    }

    private CertificationProperties properties() {
        return new CertificationProperties(
                CertificationProviderMode.STUB,
                "http://127.0.0.1:19091",
                "test-key",
                Duration.ofMillis(500),
                Duration.ofSeconds(2),
                Duration.ofHours(12),
                true,
                "benchmark-token"
        );
    }
}
