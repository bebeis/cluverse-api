package cluverse.popularity.properties;

import cluverse.common.config.PopularityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PopularityPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PopularityConfig.class);

    @Test
    void 설정이_누락되어도_안전한_기본값으로_바인딩한다() {
        contextRunner.run(context -> {
            PopularityProperties properties = context.getBean(PopularityProperties.class);

            assertThat(properties.promotionWindow()).isEqualTo(Duration.ofHours(48));
            assertThat(properties.policySampleWindow()).isEqualTo(Duration.ofDays(7));
            assertThat(properties.finalizationInterval()).isEqualTo(Duration.ofSeconds(30));
            assertThat(properties.baselineScanEnabled()).isFalse();
            assertThat(properties.experimentEndpointsEnabled()).isTrue();
        });
    }
}
