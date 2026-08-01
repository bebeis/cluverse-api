package cluverse.popularity.service.implement;

import cluverse.common.exception.ForbiddenException;
import cluverse.popularity.properties.PopularityProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PopularityExperimentAuthorizerTest {

    @Test
    void 원격_측정_토큰이_다르면_실험_실행을_거부한다() {
        PopularityExperimentAuthorizer authorizer = new PopularityExperimentAuthorizer(properties("expected"));

        assertThatThrownBy(() -> authorizer.authorize("wrong"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void 토큰이_설정되지_않은_로컬_환경은_실험_실행을_허용한다() {
        PopularityExperimentAuthorizer authorizer = new PopularityExperimentAuthorizer(properties(""));

        assertThatCode(() -> authorizer.authorize(null)).doesNotThrowAnyException();
    }

    private PopularityProperties properties(String token) {
        return new PopularityProperties(
                100L, 5, 3, 3, 2, 1,
                Duration.ofHours(48), Duration.ofDays(7), 0.98, 100, 0.30,
                Duration.ofMinutes(1), Duration.ofMinutes(1), 1_000,
                Duration.ofSeconds(30), 500,
                Duration.ofSeconds(30), 500,
                true, token
        );
    }
}
