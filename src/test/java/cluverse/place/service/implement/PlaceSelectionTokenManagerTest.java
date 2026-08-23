package cluverse.place.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.place.domain.PlaceCandidate;
import cluverse.place.domain.PlaceCategory;
import cluverse.place.domain.PlaceProvider;
import cluverse.place.properties.LocalMapProperties;
import cluverse.place.properties.PlaceProviderMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceSelectionTokenManagerTest {

    private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");

    private final LocalMapProperties properties = new LocalMapProperties(
            PlaceProviderMode.STUB,
            "http://localhost:18081",
            "client-id",
            "client-secret",
            "test-selection-token-secret-at-least-32-bytes",
            Duration.ofMinutes(15),
            Duration.ofMillis(500),
            Duration.ofSeconds(2)
    );
    private final PlaceSelectionTokenManager manager = new PlaceSelectionTokenManager(
            properties,
            new ObjectMapper(),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void 발급한_토큰은_같은_회원이_검증할_수_있다() {
        PlaceCandidate candidate = candidate();

        String token = manager.issue(10L, candidate);

        assertThat(manager.verify(10L, token).candidate()).isEqualTo(candidate);
    }

    @Test
    void 토큰이_변조되면_거부한다() {
        String token = manager.issue(10L, candidate());
        String tampered = token.substring(0, token.length() - 1) + "A";

        assertThatThrownBy(() -> manager.verify(10L, tampered))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 다른_회원의_토큰은_거부한다() {
        String token = manager.issue(10L, candidate());

        assertThatThrownBy(() -> manager.verify(11L, token))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 만료된_토큰은_거부한다() {
        String token = manager.issue(10L, candidate());
        PlaceSelectionTokenManager expiredManager = new PlaceSelectionTokenManager(
                properties,
                new ObjectMapper(),
                Clock.fixed(NOW.plus(Duration.ofMinutes(16)), ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> expiredManager.verify(10L, token))
                .isInstanceOf(BadRequestException.class);
    }

    private PlaceCandidate candidate() {
        return new PlaceCandidate(
                PlaceProvider.NAVER,
                "fingerprint",
                "클루버스 카페",
                PlaceCategory.CAFE,
                "카페,디저트",
                "서울 광진구 화양동 1",
                "서울 광진구 1",
                new BigDecimal("37.1234000"),
                new BigDecimal("127.1234000"),
                "https://map.naver.com/example"
        );
    }
}
