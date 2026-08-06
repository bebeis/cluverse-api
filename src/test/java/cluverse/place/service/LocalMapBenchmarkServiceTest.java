package cluverse.place.service;

import cluverse.place.client.StubPlaceSearchClient;
import cluverse.place.properties.LocalMapProperties;
import cluverse.place.properties.PlaceProviderMode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LocalMapBenchmarkServiceTest {

    @Test
    void in_process_mock의_상태를_readiness로_반환한다() {
        StubPlaceSearchClient stub = new StubPlaceSearchClient();
        stub.reset(300);
        LocalMapBenchmarkService service = new LocalMapBenchmarkService(properties(), Optional.of(stub));

        var response = service.readReadiness();

        assertThat(response.providerMode()).isEqualTo(PlaceProviderMode.STUB);
        assertThat(response.experimentEndpointsEnabled()).isTrue();
        assertThat(response.stubProvider()).isTrue();
        assertThat(response.stubDelayMillis()).isEqualTo(300);
        assertThat(response.stubSearchCalls()).isZero();
    }

    @Test
    void mock을_reset하면_지연과_호출_수가_초기화된다() {
        StubPlaceSearchClient stub = new StubPlaceSearchClient();
        stub.reset(0);
        stub.search("연세대 카페");
        LocalMapBenchmarkService service = new LocalMapBenchmarkService(properties(), Optional.of(stub));

        var response = service.resetStub(1000);

        assertThat(response.stubDelayMillis()).isEqualTo(1000);
        assertThat(response.stubSearchCalls()).isZero();
    }

    private LocalMapProperties properties() {
        return new LocalMapProperties(
                PlaceProviderMode.STUB,
                "http://unused.invalid",
                "",
                "",
                "test-selection-token-secret-at-least-32-bytes",
                Duration.ofMinutes(15),
                Duration.ofMillis(500),
                Duration.ofSeconds(2),
                true,
                "benchmark-token"
        );
    }
}
