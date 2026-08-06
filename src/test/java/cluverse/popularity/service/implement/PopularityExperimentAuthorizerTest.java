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
    void 설정된_토큰과_요청_토큰이_같으면_실험_실행을_허용한다() {
        PopularityExperimentAuthorizer authorizer = new PopularityExperimentAuthorizer(properties("expected"));

        assertThatCode(() -> authorizer.authorize("expected")).doesNotThrowAnyException();
    }

    @Test
    void 토큰이_설정되지_않으면_실험_실행을_허용한다() {
        PopularityExperimentAuthorizer authorizer = new PopularityExperimentAuthorizer(properties(""));

        assertThatCode(() -> authorizer.authorize(null)).doesNotThrowAnyException();
    }

    private PopularityProperties properties(String token) {
        return new PopularityProperties(
                100L, 3, 2,
                Duration.ofHours(48), Duration.ofDays(7), 0.98, 100, 0.30,
                false, 1_000,
                Duration.ofSeconds(30), 500,
                true, token
        );
    }
}
